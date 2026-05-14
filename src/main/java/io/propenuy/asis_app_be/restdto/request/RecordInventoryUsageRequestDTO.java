package io.propenuy.asis_app_be.restdto.request;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordInventoryUsageRequestDTO {
    private String usagePurpose;

    private List<BreakdownRequestDTO> breakdownsList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakdownRequestDTO {
        /** ID baris breakdown inventory (UUID) */
        private String breakdownId;
        /** Jumlah yang dipakai dari sub-item ini */
        private BigDecimal amount;
    }
}
