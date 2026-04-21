package io.propenuy.asis_app_be.restdto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemListResponseDTO {
    private List<InventoryItemSummaryResponseDTO> content;
    private int page;
    private int limit;
    private long totalElements;
    private int totalPages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryItemSummaryResponseDTO {
        private java.util.UUID id;
        private String itemName;
        private String category;
        private java.math.BigDecimal quantity;
        private String unit;
        private String donorSource;
        private java.time.LocalDateTime createdAt;
    }
}
