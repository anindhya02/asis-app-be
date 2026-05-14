package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.model.enums.ExpenseCategory;
import io.propenuy.asis_app_be.model.enums.IncomeCategory;
import io.propenuy.asis_app_be.repository.ExpenseTransactionRepository;
import io.propenuy.asis_app_be.repository.IncomeTransactionRepository;
import io.propenuy.asis_app_be.restdto.response.DonationGrowthDTO;
import io.propenuy.asis_app_be.restdto.response.DonationTrendPointDTO;
import io.propenuy.asis_app_be.restdto.response.ExecutiveFinancialResponseDTO;
import io.propenuy.asis_app_be.restdto.response.ExecutiveFinancialSelectedRangeDTO;
import io.propenuy.asis_app_be.restdto.response.ProgramVsOperationalRatioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EisExecutiveFinancialServiceImpl implements EisExecutiveFinancialService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private static final String[] ID_MONTHS = {
            "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    };

    private final IncomeTransactionRepository incomeTransactionRepository;
    private final ExpenseTransactionRepository expenseTransactionRepository;

    @Override
    @Transactional(readOnly = true)
    public ExecutiveFinancialResponseDTO getExecutiveFinancial(String period, String startDateStr, String endDateStr) {
        String p = normalizePeriod(period);

        boolean hasStart = startDateStr != null && !startDateStr.isBlank();
        boolean hasEnd = endDateStr != null && !endDateStr.isBlank();
        if (hasStart != hasEnd) {
            throw new IllegalArgumentException("startDate dan endDate harus keduanya diisi atau keduanya dikosongkan");
        }

        final LocalDate start;
        final LocalDate end;
        final boolean defaultYtdRange;
        if (!hasStart) {
            LocalDate today = LocalDate.now();
            end = today;
            start = LocalDate.of(today.getYear(), 1, 1);
            defaultYtdRange = true;
        } else {
            try {
                start = LocalDate.parse(Objects.requireNonNull(startDateStr, "startDate").trim(), ISO);
                end = LocalDate.parse(Objects.requireNonNull(endDateStr, "endDate").trim(), ISO);
            } catch (Exception e) {
                throw new IllegalArgumentException("Format startDate/endDate tidak valid. Gunakan format YYYY-MM-DD");
            }
            defaultYtdRange = false;
        }

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("startDate tidak boleh setelah endDate");
        }

        LocalDate prevStart;
        LocalDate prevEnd;
        if (defaultYtdRange) {
            prevStart = start.minusYears(1);
            prevEnd = end.minusYears(1);
        } else {
            long inclusiveDays = ChronoUnit.DAYS.between(start, end) + 1;
            prevEnd = start.minusDays(1);
            prevStart = prevEnd.minusDays(inclusiveDays - 1);
        }

        BigDecimal totalDonation = sumDonation(start, end);
        BigDecimal prevDonation = sumDonation(prevStart, prevEnd);

        List<DonationTrendPointDTO> trend = buildDonationTrend(p, start, end);
        ProgramVsOperationalRatioDTO ratio = buildProgramVsOperationalRatio(start, end);

        ExecutiveFinancialSelectedRangeDTO rangeMeta = ExecutiveFinancialSelectedRangeDTO.builder()
                .startDate(start.format(ISO))
                .endDate(end.format(ISO))
                .period(p)
                .build();

        return ExecutiveFinancialResponseDTO.builder()
                .totalDonationYTD(scaleMoney(totalDonation))
                .donationGrowth(buildDonationGrowth(totalDonation, prevDonation))
                .donationTrend(trend)
                .programVsOperationalRatio(ratio)
                .selectedRange(rangeMeta)
                .build();
    }

    private static String normalizePeriod(String period) {
        String p = period == null || period.isBlank() ? "monthly" : period.trim().toLowerCase(Locale.ROOT);
        if (!p.equals("monthly") && !p.equals("quarterly") && !p.equals("yearly")) {
            throw new IllegalArgumentException("Parameter period tidak valid (gunakan monthly, quarterly, atau yearly)");
        }
        return p;
    }

    private BigDecimal sumDonation(LocalDate start, LocalDate end) {
        return incomeTransactionRepository.sumAmountBetweenForCategory(start, end, IncomeCategory.DONASI);
    }

    private List<DonationTrendPointDTO> buildDonationTrend(String p, LocalDate start, LocalDate end) {
        Map<String, BigDecimal> raw = switch (p) {
            case "monthly" -> loadMonthlyDonationMap(start, end);
            case "quarterly" -> loadQuarterlyDonationMap(start, end);
            case "yearly" -> loadYearlyDonationMap(start, end);
            default -> throw new IllegalStateException();
        };

        List<String> orderedKeys = orderedBucketKeys(p, start, end);
        List<DonationTrendPointDTO> out = new ArrayList<>();
        for (String key : orderedKeys) {
            BigDecimal amt = raw.getOrDefault(key, ZERO);
            out.add(DonationTrendPointDTO.builder()
                    .period(key)
                    .label(labelForBucket(p, key))
                    .totalDonation(scaleMoney(amt))
                    .build());
        }
        return out;
    }

    private Map<String, BigDecimal> loadMonthlyDonationMap(LocalDate start, LocalDate end) {
        List<Object[]> rows = incomeTransactionRepository.sumDonationByYearMonthBetween(start, end);
        Map<String, BigDecimal> m = new HashMap<>();
        for (Object[] row : rows) {
            int y = ((Number) row[0]).intValue();
            int mo = ((Number) row[1]).intValue();
            String key = String.format(Locale.ROOT, "%04d-%02d", y, mo);
            m.put(key, toBigDecimal(row[2]));
        }
        return m;
    }

    private Map<String, BigDecimal> loadQuarterlyDonationMap(LocalDate start, LocalDate end) {
        List<Object[]> rows = incomeTransactionRepository.sumDonationByYearQuarterBetween(start, end);
        Map<String, BigDecimal> m = new HashMap<>();
        for (Object[] row : rows) {
            int y = ((Number) row[0]).intValue();
            int q = ((Number) row[1]).intValue();
            String key = y + "-Q" + q;
            m.put(key, toBigDecimal(row[2]));
        }
        return m;
    }

    private Map<String, BigDecimal> loadYearlyDonationMap(LocalDate start, LocalDate end) {
        List<Object[]> rows = incomeTransactionRepository.sumDonationByYearBetween(start, end);
        Map<String, BigDecimal> m = new HashMap<>();
        for (Object[] row : rows) {
            int y = ((Number) row[0]).intValue();
            String key = String.valueOf(y);
            m.put(key, toBigDecimal(row[1]));
        }
        return m;
    }

    /**
     * Menghasilkan daftar kunci interval lengkap (termasuk yang nilainya 0) agar chart berkesinambungan,
     * selaras dengan kebutuhan dashboard — berbeda dari laporan MIS yang hanya menampilkan baris non-nol.
     */
    private List<String> orderedBucketKeys(String p, LocalDate start, LocalDate end) {
        return switch (p) {
            case "monthly" -> monthlyKeys(start, end);
            case "quarterly" -> quarterlyKeys(start, end);
            case "yearly" -> yearlyKeys(start, end);
            default -> throw new IllegalStateException();
        };
    }

    private static List<String> monthlyKeys(LocalDate start, LocalDate end) {
        YearMonth ymStart = YearMonth.from(start);
        YearMonth ymEnd = YearMonth.from(end);
        List<String> keys = new ArrayList<>();
        for (YearMonth ym = ymStart; !ym.isAfter(ymEnd); ym = ym.plusMonths(1)) {
            keys.add(String.format(Locale.ROOT, "%04d-%02d", ym.getYear(), ym.getMonthValue()));
        }
        return keys;
    }

    private static List<String> quarterlyKeys(LocalDate start, LocalDate end) {
        int y = start.getYear();
        int q = quarterOf(start);
        int endY = end.getYear();
        int endQ = quarterOf(end);
        List<String> keys = new ArrayList<>();
        while (y < endY || (y == endY && q <= endQ)) {
            keys.add(y + "-Q" + q);
            q++;
            if (q > 4) {
                q = 1;
                y++;
            }
        }
        return keys;
    }

    private static int quarterOf(LocalDate d) {
        return (d.getMonthValue() - 1) / 3 + 1;
    }

    private static List<String> yearlyKeys(LocalDate start, LocalDate end) {
        List<String> keys = new ArrayList<>();
        for (int y = start.getYear(); y <= end.getYear(); y++) {
            keys.add(String.valueOf(y));
        }
        return keys;
    }

    private String labelForBucket(String p, String key) {
        return switch (p) {
            case "monthly" -> {
                String[] parts = key.split("-");
                int y = Integer.parseInt(parts[0]);
                int mo = Integer.parseInt(parts[1]);
                yield ID_MONTHS[mo] + " " + y;
            }
            case "quarterly" -> {
                int dash = key.indexOf("-Q");
                int y = Integer.parseInt(key.substring(0, dash));
                int q = Integer.parseInt(key.substring(dash + 2));
                yield "Triwulan " + q + " " + y;
            }
            case "yearly" -> key;
            default -> key;
        };
    }

    private ProgramVsOperationalRatioDTO buildProgramVsOperationalRatio(LocalDate start, LocalDate end) {
        List<Object[]> rows = expenseTransactionRepository.sumAmountByCategoryBetween(start, end);
        BigDecimal program = ZERO;
        BigDecimal operational = ZERO;
        for (Object[] row : rows) {
            ExpenseCategory cat = (ExpenseCategory) row[0];
            BigDecimal amt = toBigDecimal(row[1]);
            if (ExecutiveExpenseCategoryMapper.isProgramExpense(cat)) {
                program = program.add(amt);
            } else if (ExecutiveExpenseCategoryMapper.isOperationalExpense(cat)) {
                operational = operational.add(amt);
            }
        }
        BigDecimal total = program.add(operational);
        BigDecimal programPct = ZERO;
        BigDecimal opPct = ZERO;
        if (total.compareTo(ZERO) > 0) {
            programPct = program.multiply(ONE_HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
            opPct = operational.multiply(ONE_HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
        }
        return ProgramVsOperationalRatioDTO.builder()
                .programExpense(scaleMoney(program))
                .operationalExpense(scaleMoney(operational))
                .totalExpense(scaleMoney(total))
                .programPercentage(programPct)
                .operationalPercentage(opPct)
                .build();
    }

    private static DonationGrowthDTO buildDonationGrowth(BigDecimal current, BigDecimal previous) {
        BigDecimal cur = current != null ? current : ZERO;
        BigDecimal prev = previous != null ? previous : ZERO;

        if (prev.compareTo(ZERO) == 0 && cur.compareTo(ZERO) == 0) {
            return DonationGrowthDTO.builder()
                    .percentage(BigDecimal.ZERO.setScale(1, RoundingMode.UNNECESSARY))
                    .direction("STABLE")
                    .build();
        }
        if (prev.compareTo(ZERO) == 0) {
            return DonationGrowthDTO.builder()
                    .percentage(null)
                    .direction("UP")
                    .build();
        }

        BigDecimal pct = cur.subtract(prev).multiply(ONE_HUNDRED).divide(prev, 1, RoundingMode.HALF_UP);
        String direction = "STABLE";
        if (pct.compareTo(BigDecimal.ZERO) > 0) {
            direction = "UP";
        } else if (pct.compareTo(BigDecimal.ZERO) < 0) {
            direction = "DOWN";
        }
        return DonationGrowthDTO.builder()
                .percentage(pct)
                .direction(direction)
                .build();
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private static BigDecimal scaleMoney(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
