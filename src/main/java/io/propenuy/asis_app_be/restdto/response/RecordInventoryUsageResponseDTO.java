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
public class RecordInventoryUsageResponseDTO {
    private InventoryUsageRecordedDTO usage;
    private InventoryItemResponseDTO item;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryUsageRecordedDTO {
        private UUID id;
        private BigDecimal quantityUsed;
        private String usagePurpose;
        private String auditMessage;
        private List<InventoryItemResponseDTO.UsageLogBreakdownResponseDTO> breakdownUsages;
        private String createdByUsername;
        private LocalDateTime createdAt;
    }
}
