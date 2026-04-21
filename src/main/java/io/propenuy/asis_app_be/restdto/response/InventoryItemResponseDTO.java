package io.propenuy.asis_app_be.restdto.response;

import java.math.BigDecimal;
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
public class InventoryItemResponseDTO {
    private UUID id;
    private String itemName;
    private String category;
    private String donorSource;
    private String photoUrl;
    private BigDecimal quantity;
    private String unit;
    private List<BreakdownResponseDTO> breakdownsList;
    private String note;
    private String createdBy;
    private String createdByUsername;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakdownResponseDTO {
        private UUID id;
        private String name;
        private BigDecimal amount;
    }
}
