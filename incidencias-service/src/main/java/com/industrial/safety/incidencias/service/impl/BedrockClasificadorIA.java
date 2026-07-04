package com.industrial.safety.incidencias.service.impl;

import com.industrial.safety.incidencias.entity.Categoria;
import com.industrial.safety.incidencias.entity.Incidencia;
import com.industrial.safety.incidencias.entity.Nivel;
import com.industrial.safety.incidencias.service.ClasificadorIA;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

/**
 * Clasificador de IA sobre Amazon Bedrock (Claude Haiku vía inference profile).
 *
 * <p>Lee título + descripción + log capturado y devuelve categoría/impacto/urgencia +
 * un diagnóstico para soporte. Activo solo con {@code incidencias.ia.enabled=true}.
 *
 * <ul>
 *   <li>Fallo de Bedrock (red/servicio): {@code converse(...)} lanza excepción → se propaga
 *       para reintento/DLQ (la incidencia conserva la clasificación por reglas).</li>
 *   <li>Respuesta no parseable: se registra y se devuelve vacío (no se reintenta; se queda
 *       la clasificación por reglas).</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "incidencias.ia.enabled", havingValue = "true")
public class BedrockClasificadorIA implements ClasificadorIA {

    private static final String SYSTEM_PROMPT = """
            Eres un clasificador ITIL de incidencias de TI. Respondes SOLO con un objeto JSON.""";

    private final BedrockRuntimeClient bedrock;
    private final ObjectMapper json;

    @Value("${incidencias.ia.model-id:us.anthropic.claude-haiku-4-5-20251001-v1:0}")
    private String modelId;

    public BedrockClasificadorIA(BedrockRuntimeClient bedrock, ObjectMapper json) {
        this.bedrock = bedrock;
        this.json = json;
    }

    /** DTO intermedio de la respuesta JSON del modelo. */
    private record RespuestaIA(String categoria, String impacto, String urgencia,
                               Double confianza, String diagnostico) {
    }

    @Override
    public Optional<ClasificacionIA> clasificar(Incidencia inc) {
        String userPrompt = construirPrompt(inc);

        // Un fallo aquí (Bedrock caído) se propaga a propósito → reintento → DLQ.
        ConverseResponse resp = bedrock.converse(r -> r
                .modelId(modelId)
                .system(SystemContentBlock.fromText(SYSTEM_PROMPT))
                .messages(Message.builder()
                        .role(ConversationRole.USER)
                        .content(ContentBlock.fromText(userPrompt))
                        .build())
                .inferenceConfig(c -> c.maxTokens(500).temperature(0.0f)));

        String texto = resp.output().message().content().get(0).text();
        return parsear(texto, inc.getId());
    }

    private String construirPrompt(Incidencia inc) {
        String log = (inc.getContextoError() == null || inc.getContextoError().isBlank())
                ? "(sin log capturado)" : inc.getContextoError();
        return """
                <categorias>INFRAESTRUCTURA, APLICACIONES, BASE_DATOS, REDES_COMUNICACIONES, SEGURIDAD, DOCUMENTACION, OTROS</categorias>
                <niveles>ALTO, MEDIO, BAJO</niveles>
                <reporte>%s
                %s</reporte>
                <log>%s</log>
                Analiza el reporte y el log. Devuelve SOLO este JSON, sin texto extra:
                {"categoria": <una de categorias>, "impacto": <un nivel>, "urgencia": <un nivel>, "confianza": <0.0-1.0>, "diagnostico": "<que paso y probable causa, en 1-2 frases para soporte>"}"""
                .formatted(inc.getTitulo(), inc.getDescripcion(), log);
    }

    /** Parseo tolerante: extrae el primer objeto JSON y valida enums. Vacío si no se puede. */
    private Optional<ClasificacionIA> parsear(String texto, Long incidenciaId) {
        try {
            int ini = texto.indexOf('{');
            int fin = texto.lastIndexOf('}');
            if (ini < 0 || fin <= ini) {
                log.warn("[triaje-ia] Respuesta sin JSON para incidencia {}: {}", incidenciaId, texto);
                return Optional.empty();
            }
            RespuestaIA r = json.readValue(texto.substring(ini, fin + 1), RespuestaIA.class);
            Categoria categoria = Categoria.valueOf(r.categoria().trim().toUpperCase());
            Nivel impacto = Nivel.valueOf(r.impacto().trim().toUpperCase());
            Nivel urgencia = Nivel.valueOf(r.urgencia().trim().toUpperCase());
            double confianza = r.confianza() == null ? 0.5 : Math.clamp(r.confianza(), 0.0, 1.0);
            return Optional.of(new ClasificacionIA(categoria, impacto, urgencia, confianza, r.diagnostico()));
        } catch (Exception e) {
            log.warn("[triaje-ia] No se pudo interpretar la respuesta de la IA para incidencia {}: {}",
                    incidenciaId, e.getMessage());
            return Optional.empty();
        }
    }
}
