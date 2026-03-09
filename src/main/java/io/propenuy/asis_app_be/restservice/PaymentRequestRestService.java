package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.restdto.response.PaymentRequestListResponseDTO;

public interface PaymentRequestRestService {
    PaymentRequestListResponseDTO list(
            String startDateStr,
            String endDateStr,
            String status,
            String expenseCategory,
            String search,
            int page,
            int size,
            String currentUsername
    );
}
