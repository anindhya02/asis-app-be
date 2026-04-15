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
public class ExpenseTransactionResponseDTO {
    private UUID id;
    private LocalDate transactionDate;
    private String category;
    private String program;
    private BigDecimal amount;
    private String paymentMethod;
    private String penerimaDana;
    private String note;
    private String proofFilePath;
    private String status;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedByUsername;
}
