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
public class ProgramVsOperationalRatioDTO {
    private BigDecimal programExpense;
    private BigDecimal operationalExpense;
    private BigDecimal totalExpense;
    private BigDecimal programPercentage;
    private BigDecimal operationalPercentage;
}
