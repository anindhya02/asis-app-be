package io.propenuy.asis_app_be.restdto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeTransactionResponseDTO {
    private UUID id;
    private LocalDate transactionDate;
    private String category;
    private String sourceType;
    private String paymentMethod;
    private BigDecimal amount;
    private String donorName;
    private String note;
    private String proofFilePath;
    private String status;
    private String createdByUsername;
    private LocalDateTime createdAt;
}
