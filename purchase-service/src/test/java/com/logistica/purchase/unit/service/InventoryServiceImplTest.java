package com.logistica.purchase.unit.service;

import com.logistica.purchase.dto.InventoryItemRequest;
import com.logistica.purchase.dto.InventoryItemResponse;
import com.logistica.purchase.entity.InventoryItem;
import com.logistica.purchase.mapper.InventoryMapper;
import com.logistica.purchase.repository.InventoryRepository;
import com.logistica.purchase.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryServiceImpl — Pruebas Unitarias")
class InventoryServiceImplTest {

    @Mock InventoryRepository repository;
    @Mock InventoryMapper     mapper;

    @InjectMocks InventoryServiceImpl service;

    private InventoryItem item;
    private InventoryItemResponse response;

    @BeforeEach
    void setUp() {
        item = InventoryItem.builder()
                .id(1L)
                .codigo("EPP-001")
                .descripcion("Casco de seguridad")
                .lote("LOTE-2025")
                .vencimiento("2027-12")
                .stock(50)
                .estado("ACTIVO")
                .build();

        response = new InventoryItemResponse(1L, "EPP-001", "Casco de seguridad",
                "LOTE-2025", "2027-12", 50, "ACTIVO");
    }

    @Test
    @DisplayName("getAll: retorna todos los ítems del inventario")
    void getAll_returnsAllItems() {
        given(repository.findAll()).willReturn(List.of(item));
        given(mapper.toResponse(item)).willReturn(response);

        List<InventoryItemResponse> result = service.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).descripcion()).isEqualTo("Casco de seguridad");
        then(repository).should().findAll();
    }

    @Test
    @DisplayName("getAll: retorna lista vacía si no hay ítems")
    void getAll_empty_returnsEmptyList() {
        given(repository.findAll()).willReturn(List.of());

        List<InventoryItemResponse> result = service.getAll();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("create: guarda el ítem y retorna su respuesta")
    void create_savesAndReturnsResponse() {
        InventoryItemRequest request = new InventoryItemRequest(
                "EPP-001", "Casco de seguridad", "LOTE-2025", "2027-12", 50, "ACTIVO");

        given(mapper.toEntity(request)).willReturn(item);
        given(repository.save(item)).willReturn(item);
        given(mapper.toResponse(item)).willReturn(response);

        InventoryItemResponse result = service.create(request);

        assertThat(result.codigo()).isEqualTo("EPP-001");
        assertThat(result.stock()).isEqualTo(50);
        then(repository).should().save(any(InventoryItem.class));
    }
}
