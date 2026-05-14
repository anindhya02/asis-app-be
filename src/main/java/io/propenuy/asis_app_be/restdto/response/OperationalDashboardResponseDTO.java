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
public class OperationalDashboardResponseDTO {
    private String period;
    private int year;
    private int month;
    private BigDecimal totalFundIn;
    private BigDecimal totalFundOut;
    private BigDecimal runningBalance;
    private TicketSummaryDTO ticketSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketSummaryDTO {
        private int pending;
        private int approved;
        private int reject;
        private int revisionRequested;
    }
}
