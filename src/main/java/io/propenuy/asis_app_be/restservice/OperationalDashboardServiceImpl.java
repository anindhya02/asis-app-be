package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.model.enums.PaymentRequestStatus;
import io.propenuy.asis_app_be.repository.ExpenseTransactionRepository;
import io.propenuy.asis_app_be.repository.IncomeTransactionRepository;
import io.propenuy.asis_app_be.repository.PaymentRequestRepository;
import io.propenuy.asis_app_be.restdto.response.OperationalDashboardResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OperationalDashboardServiceImpl implements OperationalDashboardService {

    private final IncomeTransactionRepository incomeTransactionRepository;
    private final ExpenseTransactionRepository expenseTransactionRepository;
    private final PaymentRequestRepository paymentRequestRepository;

    @Override
    public OperationalDashboardResponseDTO getOperationalDashboard(String period, Integer year, Integer month) {
        String p = period == null ? "monthly" : period.trim().toLowerCase(Locale.ROOT);
        if (!p.equals("monthly")) {
            throw new IllegalArgumentException("Parameter period tidak valid (gunakan monthly)");
        }

        LocalDate now = LocalDate.now();
        int resolvedYear = year == null ? now.getYear() : year;
        int resolvedMonth = month == null ? now.getMonthValue() : month;

        if (resolvedYear < 2000 || resolvedYear > 2100) {
            throw new IllegalArgumentException("Parameter year tidak valid");
        }
        if (resolvedMonth < 1 || resolvedMonth > 12) {
            throw new IllegalArgumentException("Parameter month harus antara 1 dan 12");
        }

        LocalDate start = LocalDate.of(resolvedYear, resolvedMonth, 1);
        LocalDate end = YearMonth.of(resolvedYear, resolvedMonth).atEndOfMonth();

        BigDecimal totalFundIn = incomeTransactionRepository.sumConfirmedBetween(start, end);
        BigDecimal totalFundOut = expenseTransactionRepository.sumActiveBetween(start, end);
        BigDecimal runningBalance = totalFundIn.subtract(totalFundOut);

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59, 999_999_999);

        List<PaymentRequestStatus> statusFilter = List.of(
                PaymentRequestStatus.PENDING_REVIEW,
                PaymentRequestStatus.APPROVED,
                PaymentRequestStatus.REJECTED,
                PaymentRequestStatus.REVISION_REQUESTED
        );

        Map<PaymentRequestStatus, Integer> statusCounts = new EnumMap<>(PaymentRequestStatus.class);
        for (PaymentRequestStatus status : statusFilter) {
            statusCounts.put(status, 0);
        }

        for (Object[] row : paymentRequestRepository.countByStatusBetween(startDateTime, endDateTime, statusFilter)) {
            if (row == null || row.length < 2) {
                continue;
            }
            PaymentRequestStatus status = (PaymentRequestStatus) row[0];
            Number count = (Number) row[1];
            if (status != null && count != null) {
                statusCounts.put(status, count.intValue());
            }
        }

        OperationalDashboardResponseDTO.TicketSummaryDTO ticketSummary =
                OperationalDashboardResponseDTO.TicketSummaryDTO.builder()
                        .pending(statusCounts.getOrDefault(PaymentRequestStatus.PENDING_REVIEW, 0))
                        .approved(statusCounts.getOrDefault(PaymentRequestStatus.APPROVED, 0))
                        .reject(statusCounts.getOrDefault(PaymentRequestStatus.REJECTED, 0))
                        .revisionRequested(statusCounts.getOrDefault(PaymentRequestStatus.REVISION_REQUESTED, 0))
                        .build();

        return OperationalDashboardResponseDTO.builder()
                .period(p)
                .year(resolvedYear)
                .month(resolvedMonth)
                .totalFundIn(totalFundIn)
                .totalFundOut(totalFundOut)
                .runningBalance(runningBalance)
                .ticketSummary(ticketSummary)
                .build();
    }
}
