package com.logistica.purchase.unit.service;

import com.logistica.purchase.dto.EppDeliveredEvent;
import com.logistica.purchase.dto.EppDeliveryRequest;
import com.logistica.purchase.dto.EppDeliveryResponse;
import com.logistica.purchase.entity.EppDelivery;
import com.logistica.purchase.entity.PurchaseRequest;
import com.logistica.purchase.entity.PurchaseRequestStatus;
import com.logistica.purchase.exception.InsufficientStockException;
import com.logistica.purchase.exception.ResourceNotFoundException;
import com.logistica.purchase.mapper.EppDeliveryMapper;
import com.logistica.purchase.messaging.EppEventPublisher;
import com.logistica.purchase.repository.EppDeliveryRepository;
import com.logistica.purchase.repository.PurchaseRequestRepository;
import com.logistica.purchase.service.impl.EppDeliveryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("EppDeliveryServiceImpl — Pruebas Unitarias")
class EppDeliveryServiceImplTest {

    @Mock PurchaseRequestRepository purchaseRequestRepository;
    @Mock EppDeliveryRepository     deliveryRepository;
    @Mock EppDeliveryMapper         mapper;
    @Mock EppEventPublisher         eventPublisher;
    @Mock RestTemplate              restTemplate;

    @InjectMocks EppDeliveryServiceImpl service;

    private PurchaseRequest stockRequest;

    @BeforeEach
    void setUp() {
        stockRequest = PurchaseRequest.builder()
                .id(1L)
                .categoria("Casco")
                .cantidad(10)
                .estado(PurchaseRequestStatus.APROBADO)
                .build();
    }

    // =========================================================
    //  deliver
    // =========================================================

    @Nested
    @DisplayName("deliver")
    class DeliverTests {

        @Test
        @DisplayName("happy path: entrega EPP y descuenta stock")
        void deliver_happyPath_decreasesStockAndPublishesEvent() {
            EppDeliveryRequest request = new EppDeliveryRequest(1L, "12345678", "Juan Pérez", 3);

            EppDelivery savedDelivery = EppDelivery.builder()
                    .id(1L).inventoryItemId(1L).workerDni("12345678")
                    .workerName("Juan Pérez").cantidadEntregada(3)
                    .fechaEntrega(LocalDate.now()).build();

            EppDeliveryResponse response = new EppDeliveryResponse(
                    1L, 1L, "Casco", "12345678", "Juan Pérez", 3, LocalDate.now());

            given(purchaseRequestRepository.findById(1L)).willReturn(Optional.of(stockRequest));
            given(deliveryRepository.save(any(EppDelivery.class))).willReturn(savedDelivery);
            given(mapper.toResponse(savedDelivery)).willReturn(response);

            EppDeliveryResponse result = service.deliver(request);

            assertThat(stockRequest.getCantidad()).isEqualTo(7);
            assertThat(result.cantidadEntregada()).isEqualTo(3);
            then(purchaseRequestRepository).should().save(stockRequest);
            then(eventPublisher).should().publishDelivery(any(EppDeliveredEvent.class));
        }

        @Test
        @DisplayName("stock insuficiente lanza InsufficientStockException")
        void deliver_insufficientStock_throws() {
            EppDeliveryRequest request = new EppDeliveryRequest(1L, "12345678", "Juan Pérez", 15);

            given(purchaseRequestRepository.findById(1L)).willReturn(Optional.of(stockRequest));

            assertThatThrownBy(() -> service.deliver(request))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Disponible: 10");
        }

        @Test
        @DisplayName("stock cero lanza InsufficientStockException")
        void deliver_zeroStock_throws() {
            stockRequest.setCantidad(0);
            EppDeliveryRequest request = new EppDeliveryRequest(1L, "12345678", "Juan Pérez", 1);

            given(purchaseRequestRepository.findById(1L)).willReturn(Optional.of(stockRequest));

            assertThatThrownBy(() -> service.deliver(request))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Disponible: 0");
        }

        @Test
        @DisplayName("stock null se trata como 0 y lanza InsufficientStockException")
        void deliver_nullStock_treatedAsZero_throws() {
            stockRequest.setCantidad(null);
            EppDeliveryRequest request = new EppDeliveryRequest(1L, "12345678", "Juan Pérez", 1);

            given(purchaseRequestRepository.findById(1L)).willReturn(Optional.of(stockRequest));

            assertThatThrownBy(() -> service.deliver(request))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        @DisplayName("solicitud de compra no encontrada lanza ResourceNotFoundException")
        void deliver_purchaseRequestNotFound_throws() {
            EppDeliveryRequest request = new EppDeliveryRequest(99L, "12345678", "Juan Pérez", 1);

            given(purchaseRequestRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deliver(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("entrega exactamente el stock disponible deja cantidad en 0")
        void deliver_exactStock_leavesZero() {
            EppDeliveryRequest request = new EppDeliveryRequest(1L, "12345678", "Juan Pérez", 10);

            EppDelivery savedDelivery = EppDelivery.builder()
                    .id(1L).inventoryItemId(1L).cantidadEntregada(10)
                    .fechaEntrega(LocalDate.now()).build();
            EppDeliveryResponse response = new EppDeliveryResponse(
                    1L, 1L, "Casco", "12345678", "Juan Pérez", 10, LocalDate.now());

            given(purchaseRequestRepository.findById(1L)).willReturn(Optional.of(stockRequest));
            given(deliveryRepository.save(any())).willReturn(savedDelivery);
            given(mapper.toResponse(savedDelivery)).willReturn(response);

            service.deliver(request);

            assertThat(stockRequest.getCantidad()).isZero();
        }
    }

    // =========================================================
    //  getDeliveriesByWorkerDni
    // =========================================================

    @Test
    @DisplayName("getDeliveriesByWorkerDni: retorna entregas del trabajador")
    void getDeliveriesByWorkerDni_returnsDeliveries() {
        EppDelivery delivery = EppDelivery.builder()
                .id(1L).workerDni("12345678").cantidadEntregada(3)
                .fechaEntrega(LocalDate.now()).build();
        EppDeliveryResponse response = new EppDeliveryResponse(
                1L, 1L, "Casco", "12345678", "Juan Pérez", 3, LocalDate.now());

        given(deliveryRepository.findByWorkerDni("12345678")).willReturn(List.of(delivery));
        given(mapper.toResponse(delivery)).willReturn(response);

        List<EppDeliveryResponse> result = service.getDeliveriesByWorkerDni("12345678");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).workerDni()).isEqualTo("12345678");
    }

    @Test
    @DisplayName("getDeliveriesByWorkerDni: retorna vacío si el trabajador no tiene entregas")
    void getDeliveriesByWorkerDni_noDeliveries_returnsEmpty() {
        given(deliveryRepository.findByWorkerDni("99999999")).willReturn(List.of());

        assertThat(service.getDeliveriesByWorkerDni("99999999")).isEmpty();
    }
}
