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
public class ExecutiveFinancialResponseDTO {
    private BigDecimal totalDonationYTD;
    private DonationGrowthDTO donationGrowth;
    private List<DonationTrendPointDTO> donationTrend;
    private ProgramVsOperationalRatioDTO programVsOperationalRatio;
    private ExecutiveFinancialSelectedRangeDTO selectedRange;
}
