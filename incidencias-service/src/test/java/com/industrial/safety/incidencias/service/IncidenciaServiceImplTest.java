package com.industrial.safety.incidencias.service;

import com.industrial.safety.incidencias.dto.CrearIncidenciaRequest;
import com.industrial.safety.incidencias.dto.ResolverIncidenciaRequest;
import com.industrial.safety.incidencias.entity.Categoria;
import com.industrial.safety.incidencias.entity.EstadoIncidencia;
import com.industrial.safety.incidencias.entity.Incidencia;
import com.industrial.safety.incidencias.entity.Nivel;
import com.industrial.safety.incidencias.entity.OrigenClasificacion;
import com.industrial.safety.incidencias.entity.Prioridad;
import com.industrial.safety.incidencias.entity.SyncEstado;
import com.industrial.safety.incidencias.exception.EstadoInvalidoException;
import com.industrial.safety.incidencias.exception.ResourceNotFoundException;
import com.industrial.safety.incidencias.integration.JiraClient;
import com.industrial.safety.incidencias.mapper.IncidenciaMapper;
import com.industrial.safety.incidencias.messaging.IncidenciaEventPublisher;
import com.industrial.safety.incidencias.repository.IncidenciaRepository;
import com.industrial.safety.incidencias.service.impl.IncidenciaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IncidenciaServiceImpl — lógica de creación, aceptación y resolución")
class IncidenciaServiceImplTest {

    @Mock IncidenciaRepository repository;
    @Mock IncidenciaMapper mapper;
    @Mock IncidenciaEventPublisher publisher;
    @Mock JiraClient jiraClient;
    @Mock ClasificadorIA clasificadorIA;

    @InjectMocks IncidenciaServiceImpl service;

    private CrearIncidenciaRequest request;

    @BeforeEach
    void setUp() {
        request = new CrearIncidenciaRequest(
                Categoria.APLICACIONES, "Video no carga", "No carga el curso",
                "El reproductor queda en blanco", Nivel.ALTO, Nivel.ALTO,
                null, "Ana Pérez", "ALUMNO", null);
    }

    @Test
    @DisplayName("crear: calcula prioridad, deja REGISTRADO, asigna código y notifica")
    void crearRegistraYNotifica() {
        Incidencia entity = new Incidencia();
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(any(Incidencia.class))).thenAnswer(inv -> {
            Incidencia i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });
        when(mapper.toResponse(any(Incidencia.class))).thenReturn(null);

        service.crear(request, "kc-ana");

        assertThat(entity.getReporterId()).isEqualTo("kc-ana");
        assertThat(entity.getPrioridad()).isEqualTo(Prioridad.CRITICA);
        assertThat(entity.getEstado()).isEqualTo(EstadoIncidencia.REGISTRADO);
        assertThat(entity.getCodigo()).matches("INC-\\d{4}-001");
        assertThat(entity.getCategoriaOrigen()).isEqualTo(OrigenClasificacion.HUMANO);
        verify(publisher).notificarRegistrada(entity);
    }

    @Test
    @DisplayName("crear sin categoría: el motor de reglas la clasifica (REGLA + requiereRevision)")
    void crearClasificaPorReglas() {
        CrearIncidenciaRequest reqSinCat = new CrearIncidenciaRequest(
                null, null, "No puedo crear cupones", "Al intentar crear un cupón de descuento me sale un error",
                null, null, null, "Marketing", "MARKETING", null);
        Incidencia entity = new Incidencia();
        when(mapper.toEntity(reqSinCat)).thenReturn(entity);
        when(repository.save(any(Incidencia.class))).thenAnswer(inv -> {
            Incidencia i = inv.getArgument(0);
            i.setId(2L);
            return i;
        });
        when(mapper.toResponse(any(Incidencia.class))).thenReturn(null);

        service.crear(reqSinCat, "kc-mkt");

        assertThat(entity.getCategoria()).isEqualTo(Categoria.APLICACIONES);
        assertThat(entity.getCategoriaOrigen()).isEqualTo(OrigenClasificacion.REGLA);
        assertThat(entity.getRequiereRevision()).isTrue();
        assertThat(entity.getImpacto()).isEqualTo(Nivel.MEDIO);
        assertThat(entity.getPrioridad()).isEqualTo(Prioridad.MEDIA);
    }

    @Test
    @DisplayName("aceptar: REGISTRADO → EN_ATENCION y registra quién atiende")
    void aceptarTransiciona() {
        Incidencia inc = Incidencia.builder().id(5L).estado(EstadoIncidencia.REGISTRADO).build();
        when(repository.findById(5L)).thenReturn(Optional.of(inc));

        service.aceptar(5L, "kc-admin");

        assertThat(inc.getEstado()).isEqualTo(EstadoIncidencia.EN_ATENCION);
        assertThat(inc.getAtendidoPor()).isEqualTo("kc-admin");
        assertThat(inc.getAceptadoEn()).isNotNull();
    }

    @Test
    @DisplayName("aceptar: estado distinto de REGISTRADO lanza EstadoInvalidoException")
    void aceptarEnEstadoInvalido() {
        Incidencia inc = Incidencia.builder().id(5L).estado(EstadoIncidencia.EN_ATENCION).build();
        when(repository.findById(5L)).thenReturn(Optional.of(inc));

        assertThatThrownBy(() -> service.aceptar(5L, "kc-admin"))
                .isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    @DisplayName("resolver: EN_ATENCION → RESUELTO, guarda solución y notifica")
    void resolverTransicionaYNotifica() {
        Incidencia inc = Incidencia.builder().id(7L).estado(EstadoIncidencia.EN_ATENCION).build();
        when(repository.findById(7L)).thenReturn(Optional.of(inc));
        ResolverIncidenciaRequest req = new ResolverIncidenciaRequest("Se reinició el pod", true);

        service.resolver(7L, req, "kc-admin");

        assertThat(inc.getEstado()).isEqualTo(EstadoIncidencia.RESUELTO);
        assertThat(inc.getResolucionDescripcion()).isEqualTo("Se reinició el pod");
        assertThat(inc.getResueltoBien()).isTrue();
        assertThat(inc.getResueltoEn()).isNotNull();
        verify(publisher).notificarResuelta(inc);
    }

    @Test
    @DisplayName("resolver: no EN_ATENCION lanza EstadoInvalidoException")
    void resolverEnEstadoInvalido() {
        Incidencia inc = Incidencia.builder().id(7L).estado(EstadoIncidencia.REGISTRADO).build();
        when(repository.findById(7L)).thenReturn(Optional.of(inc));
        ResolverIncidenciaRequest req = new ResolverIncidenciaRequest("x", false);

        assertThatThrownBy(() -> service.resolver(7L, req, "kc-admin"))
                .isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    @DisplayName("getById: inexistente lanza ResourceNotFoundException")
    void getByIdInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("sincronizar: marca PENDIENTE y encola el evento de sync")
    void sincronizarEncola() {
        Incidencia inc = Incidencia.builder().id(3L).build();
        when(repository.findById(3L)).thenReturn(Optional.of(inc));
        when(mapper.toResponse(any(Incidencia.class))).thenReturn(null);

        service.sincronizar(3L, "kc-admin");

        assertThat(inc.getSyncEstado()).isEqualTo(SyncEstado.PENDIENTE);
        verify(publisher).solicitarSync(3L);
    }

    @Test
    @DisplayName("procesarSync: éxito deja SINCRONIZADO con ticketId y url")
    void procesarSyncExito() {
        Incidencia inc = Incidencia.builder().id(4L).codigo("INC-2026-004").build();
        when(repository.findById(4L)).thenReturn(Optional.of(inc));
        when(jiraClient.crearTicket(inc))
                .thenReturn(new JiraClient.JiraTicket(123L, "GES-1", "https://integrador.atlassian.net/browse/GES-1"));

        service.procesarSync(4L);

        assertThat(inc.getFreshserviceTicketId()).isEqualTo(123L);
        assertThat(inc.getFreshserviceUrl()).isEqualTo("https://integrador.atlassian.net/browse/GES-1");
        assertThat(inc.getSyncEstado()).isEqualTo(SyncEstado.SINCRONIZADO);
    }

    @Test
    @DisplayName("procesarTriaje: la IA refina la clasificación por reglas (origen IA, prioridad recalculada)")
    void procesarTriajeRefinaReglas() {
        Incidencia inc = Incidencia.builder()
                .id(8L).categoria(Categoria.OTROS).categoriaOrigen(OrigenClasificacion.REGLA)
                .impacto(Nivel.MEDIO).urgencia(Nivel.MEDIO).build();
        when(repository.findById(8L)).thenReturn(Optional.of(inc));
        when(clasificadorIA.clasificar(inc)).thenReturn(Optional.of(
                new ClasificadorIA.ClasificacionIA(Categoria.BASE_DATOS, Nivel.ALTO, Nivel.ALTO,
                        0.9, "Timeout de conexión a la BD")));

        service.procesarTriaje(8L);

        assertThat(inc.getCategoria()).isEqualTo(Categoria.BASE_DATOS);
        assertThat(inc.getCategoriaOrigen()).isEqualTo(OrigenClasificacion.IA);
        assertThat(inc.getPrioridad()).isEqualTo(Prioridad.CRITICA);
        assertThat(inc.getIaDiagnostico()).isEqualTo("Timeout de conexión a la BD");
        assertThat(inc.getRequiereRevision()).isFalse();
    }

    @Test
    @DisplayName("procesarTriaje: respeta la categoría elegida por un humano; solo agrega el diagnóstico")
    void procesarTriajeRespetaHumano() {
        Incidencia inc = Incidencia.builder()
                .id(9L).categoria(Categoria.APLICACIONES).categoriaOrigen(OrigenClasificacion.HUMANO)
                .build();
        when(repository.findById(9L)).thenReturn(Optional.of(inc));
        when(clasificadorIA.clasificar(inc)).thenReturn(Optional.of(
                new ClasificadorIA.ClasificacionIA(Categoria.BASE_DATOS, Nivel.ALTO, Nivel.ALTO,
                        0.95, "diagnóstico IA")));

        service.procesarTriaje(9L);

        assertThat(inc.getCategoria()).isEqualTo(Categoria.APLICACIONES);
        assertThat(inc.getCategoriaOrigen()).isEqualTo(OrigenClasificacion.HUMANO);
        assertThat(inc.getIaDiagnostico()).isEqualTo("diagnóstico IA");
    }

    @Test
    @DisplayName("procesarTriaje: IA sin resultado (deshabilitada) no cambia la clasificación")
    void procesarTriajeSinResultado() {
        Incidencia inc = Incidencia.builder()
                .id(10L).categoria(Categoria.OTROS).categoriaOrigen(OrigenClasificacion.REGLA).build();
        when(repository.findById(10L)).thenReturn(Optional.of(inc));
        when(clasificadorIA.clasificar(inc)).thenReturn(Optional.empty());

        service.procesarTriaje(10L);

        assertThat(inc.getCategoria()).isEqualTo(Categoria.OTROS);
        assertThat(inc.getCategoriaOrigen()).isEqualTo(OrigenClasificacion.REGLA);
        assertThat(inc.getIaDiagnostico()).isNull();
    }

    @Test
    @DisplayName("marcarSyncError: deja ERROR con el mensaje")
    void marcarSyncError() {
        Incidencia inc = Incidencia.builder().id(5L).build();
        when(repository.findById(5L)).thenReturn(Optional.of(inc));

        service.marcarSyncError(5L, "Freshservice caído");

        assertThat(inc.getSyncEstado()).isEqualTo(SyncEstado.ERROR);
        assertThat(inc.getSyncError()).isEqualTo("Freshservice caído");
    }
}
