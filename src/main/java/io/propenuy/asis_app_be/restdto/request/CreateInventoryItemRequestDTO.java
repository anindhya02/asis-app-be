package io.propenuy.asis_app_be.restdto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInventoryItemRequestDTO {
    private String itemName;
    private String category;
    private String donorSource;
    private String photoUrl;
    private String quantity;
    private String unit;
    private List<BreakdownRequestDTO> breakdownsList;
    private String note;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakdownRequestDTO {
        private String name;
        private String amount;
    }
}
