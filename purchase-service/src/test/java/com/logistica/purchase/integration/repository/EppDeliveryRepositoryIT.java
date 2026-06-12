package com.logistica.purchase.integration.repository;

import com.logistica.purchase.entity.EppDelivery;
import com.logistica.purchase.repository.EppDeliveryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Tag("integration")
@Testcontainers
// @DataJpaTest no hereda BasePurchaseIT: hay que desactivar el Parameter Store
// aquí, si no spring.config.import=aws-parameterstore intenta cargar credenciales.
@TestPropertySource(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false"
})
@DisplayName("EppDeliveryRepository — Pruebas de Integración con PostgreSQL")
class EppDeliveryRepositoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    EppDeliveryRepository repository;

    private static final String DNI_WORKER_A = "12345678";
    private static final String DNI_WORKER_B = "87654321";

    @BeforeEach
    void setUp() {
        repository.save(EppDelivery.builder()
                .inventoryItemId(1L)
                .inventoryItemDescripcion("Casco")
                .workerDni(DNI_WORKER_A)
                .workerName("Juan Pérez")
                .cantidadEntregada(2)
                .fechaEntrega(LocalDate.now())
                .build());

        repository.save(EppDelivery.builder()
                .inventoryItemId(2L)
                .inventoryItemDescripcion("Guante")
                .workerDni(DNI_WORKER_A)
                .workerName("Juan Pérez")
                .cantidadEntregada(1)
                .fechaEntrega(LocalDate.now().minusDays(1))
                .build());

        repository.save(EppDelivery.builder()
                .inventoryItemId(1L)
                .inventoryItemDescripcion("Casco")
                .workerDni(DNI_WORKER_B)
                .workerName("María García")
                .cantidadEntregada(3)
                .fechaEntrega(LocalDate.now())
                .build());
    }

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("findByWorkerDni: retorna todas las entregas del trabajador A")
    void findByWorkerDni_returnsDeliveriesForWorkerA() {
        List<EppDelivery> result = repository.findByWorkerDni(DNI_WORKER_A);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(d -> d.getWorkerDni().equals(DNI_WORKER_A));
    }

    @Test
    @DisplayName("findByWorkerDni: retorna solo las entregas del trabajador B")
    void findByWorkerDni_returnsDeliveriesForWorkerB() {
        List<EppDelivery> result = repository.findByWorkerDni(DNI_WORKER_B);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWorkerName()).isEqualTo("María García");
    }

    @Test
    @DisplayName("findByWorkerDni: lista vacía para DNI sin entregas")
    void findByWorkerDni_unknownDni_returnsEmpty() {
        List<EppDelivery> result = repository.findByWorkerDni("00000000");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save y findById: persistencia básica correcta")
    void saveAndFindById_persists() {
        EppDelivery saved = repository.save(EppDelivery.builder()
                .inventoryItemId(3L)
                .inventoryItemDescripcion("Chaleco")
                .workerDni("11111111")
                .workerName("Carlos López")
                .cantidadEntregada(1)
                .fechaEntrega(LocalDate.now())
                .build());

        assertThat(repository.findById(saved.getId())).isPresent();
    }
}
