package io.propenuy.asis_app_be.restservice;

import org.springframework.web.multipart.MultipartFile;

import io.propenuy.asis_app_be.restdto.response.PaymentRequestListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.PaymentRequestResponseDTO;

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

    PaymentRequestResponseDTO create(
            String title,
            String neededDate,
            String expenseCategory,
            String subCategory,
            String amountStr,
            String paymentMethod,
            String notes,
            String breakdownListJson,
            String submitStr,
            MultipartFile supportingDocument,
            String currentUsername
    );
}
