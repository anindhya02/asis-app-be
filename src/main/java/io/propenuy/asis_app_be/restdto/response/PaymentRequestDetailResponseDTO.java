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
public class PaymentRequestDetailResponseDTO {
    private UUID id;
    private String title;
    private String expenseCategory;
    private String expenseSubCategory;
    private BigDecimal amount;
    private List<BreakdownItemDTO> breakdownList;
    private LocalDate neededDate;
    private String paymentMethod;
    private String notes;
    private List<AttachmentDTO> attachments;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedByUsername;
    private CreatedByDTO createdBy;
    private List<ReviewHistoryItemDTO> reviewHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakdownItemDTO {
        private UUID id;
        private String description;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentDTO {
        private String url;
        private String fileName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatedByDTO {
        private String username;
        private String name;
        private String role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewHistoryItemDTO {
        private String status;
        private String actorName;
        private String actorRole;
        private String actorUsername;
        private String note;
        private LocalDateTime occurredAt;
    }
}
