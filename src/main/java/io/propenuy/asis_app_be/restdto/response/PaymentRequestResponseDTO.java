package io.propenuy.asis_app_be.restdto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String subCategory;
    private String program;
    private LocalDate neededDate;
    private String paymentMethod;
    private String recipient;
    private String notes;
    private String supportingDocumentUrl;
    private String supportingDocumentName;
    private List<BreakdownItemDTO> breakdowns;
    private String status;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakdownItemDTO {
        private UUID id;
        private String description;
        private BigDecimal amount;
    }
}
