package com.logistica.purchase.unit.mapper;

import com.logistica.purchase.dto.InventoryItemRequest;
import com.logistica.purchase.dto.InventoryItemResponse;
import com.logistica.purchase.dto.PurchaseRequestCreateRequest;
import com.logistica.purchase.dto.PurchaseRequestResponse;
import com.logistica.purchase.dto.EppDeliveryResponse;
import com.logistica.purchase.entity.EppDelivery;
import com.logistica.purchase.entity.InventoryItem;
import com.logistica.purchase.entity.PurchaseRequest;
import com.logistica.purchase.entity.PurchaseRequestStatus;
import com.logistica.purchase.mapper.EppDeliveryMapperImpl;
import com.logistica.purchase.mapper.InventoryMapperImpl;
import com.logistica.purchase.mapper.PurchaseRequestMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de los mappers MapStruct.
 *
 * Se instancian directamente los *Impl generados (componentModel = "spring" igual
 * genera una clase pública con constructor sin argumentos) y se invocan con
 * argumento null y con objeto poblado, para cubrir ambas ramas del null-check que
 * MapStruct genera en cada método.
 */
@DisplayName("Mappers MapStruct — Pruebas Unitarias")
class PurchaseMappersTest {

    private final PurchaseRequestMapperImpl purchaseMapper = new PurchaseRequestMapperImpl();
    private final InventoryMapperImpl       inventoryMapper = new InventoryMapperImpl();
    private final EppDeliveryMapperImpl     eppMapper       = new EppDeliveryMapperImpl();

    // ===================== PurchaseRequestMapper =====================

    @Test
    @DisplayName("PurchaseRequestMapper: null -> null en ambos sentidos")
    void purchaseMapper_nullInputs_returnNull() {
        assertThat(purchaseMapper.toEntity(null)).isNull();
        assertThat(purchaseMapper.toResponse(null)).isNull();
    }

    @Test
    @DisplayName("PurchaseRequestMapper: mapea request -> entity poblada")
    void purchaseMapper_toEntity_populated() {
        PurchaseRequestCreateRequest request = new PurchaseRequestCreateRequest(
                "SC-100", LocalDate.of(2026, 1, 1), "Casco", 10, "Prov", 500.0, "just");

        PurchaseRequest entity = purchaseMapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getCodigoSolicitud()).isEqualTo("SC-100");
        assertThat(entity.getCategoria()).isEqualTo("Casco");
        assertThat(entity.getCantidad()).isEqualTo(10);
    }

    @Test
    @DisplayName("PurchaseRequestMapper: mapea entity -> response poblada (estado a String)")
    void purchaseMapper_toResponse_populated() {
        PurchaseRequest entity = PurchaseRequest.builder()
                .id(1L).codigoSolicitud("SC-100").fecha(LocalDate.of(2026, 1, 1))
                .categoria("Casco").cantidad(10).proveedor("Prov").costoEstimado(500.0)
                .justificacion("just").estado(PurchaseRequestStatus.PENDIENTE)
                .build();

        PurchaseRequestResponse response = purchaseMapper.toResponse(entity);

        assertThat(response).isNotNull();
        assertThat(response.codigoSolicitud()).isEqualTo("SC-100");
        assertThat(response.estado()).isEqualTo("PENDIENTE");
    }

    // ===================== InventoryMapper =====================

    @Test
    @DisplayName("InventoryMapper: null -> null en ambos sentidos")
    void inventoryMapper_nullInputs_returnNull() {
        assertThat(inventoryMapper.toEntity(null)).isNull();
        assertThat(inventoryMapper.toResponse(null)).isNull();
    }

    @Test
    @DisplayName("InventoryMapper: mapea request -> entity y entity -> response")
    void inventoryMapper_populated() {
        InventoryItemRequest request = new InventoryItemRequest(
                "INV-1", "Casco MSA", "L-1", "2027-01-01", 50, "DISPONIBLE");

        InventoryItem entity = inventoryMapper.toEntity(request);
        assertThat(entity).isNotNull();
        assertThat(entity.getCodigo()).isEqualTo("INV-1");
        assertThat(entity.getStock()).isEqualTo(50);

        InventoryItem stored = InventoryItem.builder()
                .id(1L).codigo("INV-1").descripcion("Casco MSA").lote("L-1")
                .vencimiento("2027-01-01").stock(50).estado("DISPONIBLE").build();

        InventoryItemResponse response = inventoryMapper.toResponse(stored);
        assertThat(response).isNotNull();
        assertThat(response.codigo()).isEqualTo("INV-1");
    }

    // ===================== EppDeliveryMapper =====================

    @Test
    @DisplayName("EppDeliveryMapper: null -> null")
    void eppMapper_nullInput_returnsNull() {
        assertThat(eppMapper.toResponse(null)).isNull();
    }

    @Test
    @DisplayName("EppDeliveryMapper: mapea entity -> response poblada")
    void eppMapper_toResponse_populated() {
        EppDelivery entity = EppDelivery.builder()
                .id(1L).inventoryItemId(5L).inventoryItemDescripcion("Casco")
                .workerDni("12345678").workerName("Juan").cantidadEntregada(2)
                .fechaEntrega(LocalDate.of(2026, 1, 1)).build();

        EppDeliveryResponse response = eppMapper.toResponse(entity);

        assertThat(response).isNotNull();
        assertThat(response.workerDni()).isEqualTo("12345678");
    }
}
