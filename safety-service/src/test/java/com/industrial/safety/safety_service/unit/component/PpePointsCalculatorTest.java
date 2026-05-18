package com.industrial.safety.safety_service.unit.component;

import com.industrial.safety.safety_service.config.SafetyPointsProperties;
import com.industrial.safety.safety_service.service.PpePointsCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PpePointsCalculator — Pruebas Unitarias")
class PpePointsCalculatorTest {

    @Mock
    SafetyPointsProperties properties;

    @InjectMocks
    PpePointsCalculator calculator;

    @BeforeEach
    void setUp() {
        given(properties.cascoOrDefault()).willReturn(20);
        given(properties.guantesOrDefault()).willReturn(5);
        given(properties.chalecoOrDefault()).willReturn(10);
    }

    @Test
    @DisplayName("lista null retorna 0 puntos")
    void totalDeduction_nullList_returnsZero() {
        assertThat(calculator.totalDeduction(null)).isZero();
    }

    @Test
    @DisplayName("lista vacía retorna 0 puntos")
    void totalDeduction_emptyList_returnsZero() {
        assertThat(calculator.totalDeduction(List.of())).isZero();
    }

    @Test
    @DisplayName("casco descuenta 20 puntos")
    void totalDeduction_casco_returns20() {
        assertThat(calculator.totalDeduction(List.of("Casco"))).isEqualTo(20);
    }

    @Test
    @DisplayName("guante descuenta 5 puntos")
    void totalDeduction_guante_returns5() {
        assertThat(calculator.totalDeduction(List.of("Guante"))).isEqualTo(5);
    }

    @Test
    @DisplayName("chaleco descuenta 10 puntos")
    void totalDeduction_chaleco_returns10() {
        assertThat(calculator.totalDeduction(List.of("Chaleco"))).isEqualTo(10);
    }

    @Test
    @DisplayName("vestimenta descuenta 10 puntos (alias de chaleco)")
    void totalDeduction_vestimenta_returns10() {
        assertThat(calculator.totalDeduction(List.of("Vestimenta"))).isEqualTo(10);
    }

    @Test
    @DisplayName("casco + guante + chaleco suman 35 puntos")
    void totalDeduction_multiple_sumsDeductions() {
        assertThat(calculator.totalDeduction(List.of("Casco", "Guante", "Chaleco"))).isEqualTo(35);
    }

    @Test
    @DisplayName("etiqueta con tilde se normaliza (Cásco → casco)")
    void totalDeduction_accentNormalized() {
        assertThat(calculator.totalDeduction(List.of("Cásco"))).isEqualTo(20);
    }

    @Test
    @DisplayName("etiqueta desconocida retorna 0 sin fallo")
    void totalDeduction_unknownLabel_returnsZero() {
        assertThat(calculator.totalDeduction(List.of("botas_seguridad"))).isZero();
    }

    @Test
    @DisplayName("mayúsculas y minúsculas se normalizan correctamente")
    void totalDeduction_caseMixedLabel_recognized() {
        assertThat(calculator.totalDeduction(List.of("CASCO"))).isEqualTo(20);
        assertThat(calculator.totalDeduction(List.of("guante"))).isEqualTo(5);
    }
}
