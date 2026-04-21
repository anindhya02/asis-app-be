package io.propenuy.asis_app_be.restdto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialReportBreakdownRowDTO {
    /** INCOME | EXPENSE */
    private String rowType;
    private String label;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netDifference;
}
