package io.propenuy.asis_app_be.restdto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestResponseDTO {
    private UUID id;
    private String title;
    private String purpose;
    private BigDecimal amount;
    private String expenseCategory;
    private String program;
    private String status;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
