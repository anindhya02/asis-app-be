package io.propenuy.asis_app_be.restdto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialReportResponseDTO {
    private String period;
    private int year;
    private Integer month;
    private Integer quarter;
    private String periodLabel;
    private DateRangeDTO dateRange;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netDifference;
    private List<String> selectedCategoryIds;
    private List<CategoryOptionDTO> availableCategories;
    private List<FinancialReportBreakdownRowDTO> breakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRangeDTO {
        private String startDate;
        private String endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryOptionDTO {
        private String id;
        private String label;
    }
}
