package com.industrial.safety.eventos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Politicas de deteccion configurables (diapositiva 30 del material S15/S29).
 *
 * <p>Cada entrada del mapa {@code umbrales} asocia una metrica a una lista de bandas
 * en formato {@code "desde:NIVEL"} (ej. {@code "95:CRITICAL"}). Lo que se define aqui
 * (application.yaml o SSM {@code /config/eventos-service/}) se fusiona sobre los
 * defaults de {@link com.industrial.safety.eventos.service.PoliticaUmbrales}, de modo
 * que las politicas se ajustan sin tocar codigo.
 */
@ConfigurationProperties(prefix = "eventos")
public class UmbralProperties {

    /** metrica -> lista de bandas "desde:NIVEL". Vacio = usa solo los defaults. */
    private Map<String, List<String>> umbrales = new LinkedHashMap<>();

    public Map<String, List<String>> getUmbrales() {
        return umbrales;
    }

    public void setUmbrales(Map<String, List<String>> umbrales) {
        this.umbrales = umbrales;
    }
}
