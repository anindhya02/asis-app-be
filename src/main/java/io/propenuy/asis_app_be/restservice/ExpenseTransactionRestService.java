package io.propenuy.asis_app_be.restservice;

import org.springframework.web.multipart.MultipartFile;

import io.propenuy.asis_app_be.restdto.response.ExpenseTransactionListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.ExpenseTransactionResponseDTO;

public interface ExpenseTransactionRestService {
    ExpenseTransactionResponseDTO create(
            String transactionDateStr,
            String category,
            String subCategory,
            String paymentMethod,
            String amountStr,
            String note,
            MultipartFile proofFile,
            String currentUsername
    );

    ExpenseTransactionListResponseDTO list(
            String startDateStr,
            String endDateStr,
            String category,
            String paymentMethod,
            String search,
            int page,
            int size
    );

    ExpenseTransactionResponseDTO getById(String id);

    ExpenseTransactionResponseDTO update(
            String id,
            String category,
            String subCategory,
            String note,
            String currentUsername
    );

    void softDelete(String id, String currentUsername);
}
