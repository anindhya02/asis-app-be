package io.propenuy.asis_app_be.restservice;

import org.springframework.web.multipart.MultipartFile;

import io.propenuy.asis_app_be.restdto.response.ExpenseTransactionListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.ExpenseTransactionResponseDTO;

public interface ExpenseTransactionRestService {
    ExpenseTransactionResponseDTO create(
            String transactionDateStr,
            String category,
            String program,
            String paymentMethod,
            String amountStr,
            String penerimaDana,
            String note,
            MultipartFile proofFile,
            String currentUsername
    );

    ExpenseTransactionListResponseDTO list(
            String startDateStr,
            String endDateStr,
            String paymentMethod,
            String search,
            int page,
            int size
    );
}
