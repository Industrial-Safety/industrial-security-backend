package com.logistica.purchase.service;

import com.logistica.purchase.dto.EppDeliveryRequest;
import com.logistica.purchase.dto.EppDeliveryResponse;
import com.logistica.purchase.dto.WorkerResponse;
import java.util.List;

public interface EppDeliveryService {
    WorkerResponse searchWorker(String dni);
    EppDeliveryResponse deliver(EppDeliveryRequest request);
    List<EppDeliveryResponse> getDeliveriesByWorkerDni(String workerDni);
}
