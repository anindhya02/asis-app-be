package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.restdto.response.IncomeTransactionListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.IncomeTransactionResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface IncomeTransactionRestService {
    IncomeTransactionResponseDTO create(
            String transactionDateStr,
            String category,
            String sourceType,
            String paymentMethod,
            String amountStr,
            String note,
            String donorName,
            MultipartFile proofFile,
            String currentUsername
    );

    IncomeTransactionListResponseDTO list(
            String startDateStr,
            String endDateStr,
            String category,
            String paymentMethod,
            String sourceType,
            int page,
            int size
    );
}
