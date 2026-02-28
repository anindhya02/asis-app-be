package io.propenuy.asis_app_be.restdto.request;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for multipart form-data (excluding proof file which is sent as separate part).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateIncomeTransactionRequestDTO {
    private LocalDate transactionDate;
    private String category;      // DONASI, ZAKAT, INFAQ, LAIN_LAIN
    private String sourceType;
    private String paymentMethod; // CASH, TRANSFER
    private BigDecimal amount;
    private String note;
    private String donorName;
}
