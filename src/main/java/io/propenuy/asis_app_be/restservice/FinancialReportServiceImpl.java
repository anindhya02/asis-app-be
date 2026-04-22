package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.model.enums.ExpenseCategory;
import io.propenuy.asis_app_be.model.enums.IncomeCategory;
import io.propenuy.asis_app_be.repository.ExpenseTransactionRepository;
import io.propenuy.asis_app_be.repository.IncomeTransactionRepository;
import io.propenuy.asis_app_be.restdto.response.FinancialReportBreakdownRowDTO;
import io.propenuy.asis_app_be.restdto.response.FinancialReportResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FinancialReportServiceImpl implements FinancialReportService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String[] ID_MONTHS = {
            "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    };

    private final IncomeTransactionRepository incomeTransactionRepository;
    private final ExpenseTransactionRepository expenseTransactionRepository;

    @Override
    public FinancialReportResponseDTO getFinancialReport(
            String period,
            int year,
            Integer month,
            Integer quarter,
            String categoryIds,
            String programId
    ) {
        if (programId != null && !programId.isBlank()) {
            throw new IllegalArgumentException("Filter program belum didukung");
        }

        String p = period == null ? "" : period.trim().toLowerCase(Locale.ROOT);
        if (!p.equals("monthly") && !p.equals("quarterly") && !p.equals("yearly")) {
            throw new IllegalArgumentException("Parameter period tidak valid (gunakan monthly, quarterly, atau yearly)");
        }

        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Parameter year tidak valid");
        }

        LocalDate start;
        LocalDate end;
        String periodLabel;

        switch (p) {
            case "monthly" -> {
                if (month == null) {
                    throw new IllegalArgumentException("Parameter month wajib diisi untuk period monthly");
                }
                if (month < 1 || month > 12) {
                    throw new IllegalArgumentException("Parameter month harus antara 1 dan 12");
                }
                start = LocalDate.of(year, month, 1);
                end = YearMonth.of(year, month).atEndOfMonth();
                periodLabel = ID_MONTHS[month] + " " + year;
            }
            case "quarterly" -> {
                if (quarter == null) {
                    throw new IllegalArgumentException("Parameter quarter wajib diisi untuk period quarterly");
                }
                if (quarter < 1 || quarter > 4) {
                    throw new IllegalArgumentException("Parameter quarter harus antara 1 dan 4");
                }
                int firstMonth = (quarter - 1) * 3 + 1;
                start = LocalDate.of(year, firstMonth, 1);
                end = start.plusMonths(3).minusDays(1);
                periodLabel = "Triwulan " + quarter + " " + year;
            }
            case "yearly" -> {
                start = LocalDate.of(year, 1, 1);
                end = LocalDate.of(year, 12, 31);
                periodLabel = String.valueOf(year);
            }
            default -> throw new IllegalStateException();
        }

        Set<String> selectedCategorySet = new LinkedHashSet<>();
        if (categoryIds != null && !categoryIds.isBlank()) {
            Arrays.stream(categoryIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(s -> s.toUpperCase(Locale.ROOT))
                    .forEach(selectedCategorySet::add);
        }

        List<IncomeCategory> selectedIncomeCategories = new ArrayList<>();
        List<ExpenseCategory> selectedExpenseCategories = new ArrayList<>();
        for (String selectedCategory : selectedCategorySet) {
            boolean found = false;
            try {
                selectedIncomeCategories.add(IncomeCategory.valueOf(selectedCategory));
                found = true;
            } catch (IllegalArgumentException ignored) {}
            try {
                selectedExpenseCategories.add(ExpenseCategory.valueOf(selectedCategory));
                found = true;
            } catch (IllegalArgumentException ignored) {}

            if (!found) {
                throw new IllegalArgumentException("Parameter categoryIds tidak valid");
            }
        }

        List<Object[]> incomeRows;
        List<Object[]> expenseRows;
        BigDecimal totalIncome;
        BigDecimal totalExpense;

        if (selectedIncomeCategories.isEmpty() && selectedExpenseCategories.isEmpty()) {
            incomeRows = incomeTransactionRepository.sumAmountByCategoryBetween(start, end);
            totalIncome = incomeRows.stream()
                    .map(row -> (BigDecimal) row[1])
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            incomeRows = new ArrayList<>();
            totalIncome = BigDecimal.ZERO;
            for (IncomeCategory category : selectedIncomeCategories) {
                BigDecimal incomeAmount = incomeTransactionRepository.sumAmountBetweenForCategory(start, end, category);
                totalIncome = totalIncome.add(incomeAmount);
                if (incomeAmount.compareTo(BigDecimal.ZERO) > 0) {
                    incomeRows.add(new Object[]{category, incomeAmount});
                }
            }
        }

        if (selectedIncomeCategories.isEmpty() && selectedExpenseCategories.isEmpty()) {
            expenseRows = expenseTransactionRepository.sumAmountByCategoryBetween(start, end);
            totalExpense = expenseRows.stream()
                    .map(row -> (BigDecimal) row[1])
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            expenseRows = new ArrayList<>();
            totalExpense = BigDecimal.ZERO;
            for (ExpenseCategory category : selectedExpenseCategories) {
                BigDecimal expenseAmount = expenseTransactionRepository.sumAmountBetweenForCategory(start, end, category);
                totalExpense = totalExpense.add(expenseAmount);
                if (expenseAmount.compareTo(BigDecimal.ZERO) > 0) {
                    expenseRows.add(new Object[]{category, expenseAmount});
                }
            }
        }

        BigDecimal netDifference = totalIncome.subtract(totalExpense);

        List<FinancialReportBreakdownRowDTO> breakdown = new ArrayList<>();
        for (Object[] row : incomeRows) {
            IncomeCategory cat = (IncomeCategory) row[0];
            BigDecimal amt = (BigDecimal) row[1];
            if (amt.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            breakdown.add(FinancialReportBreakdownRowDTO.builder()
                    .rowType("INCOME")
                    .label(formatIncomeCategory(cat))
                    .totalIncome(amt)
                    .totalExpense(BigDecimal.ZERO)
                    .netDifference(amt)
                    .build());
        }
        for (Object[] row : expenseRows) {
            ExpenseCategory cat = (ExpenseCategory) row[0];
            BigDecimal amt = (BigDecimal) row[1];
            if (amt.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            breakdown.add(FinancialReportBreakdownRowDTO.builder()
                    .rowType("EXPENSE")
                    .label(formatExpenseCategory(cat))
                    .totalIncome(BigDecimal.ZERO)
                    .totalExpense(amt)
                    .netDifference(amt.negate())
                    .build());
        }

        breakdown.sort(Comparator
                .comparing((FinancialReportBreakdownRowDTO r) -> "INCOME".equals(r.getRowType()) ? 0 : 1)
                .thenComparing(FinancialReportBreakdownRowDTO::getLabel, String.CASE_INSENSITIVE_ORDER));

        return FinancialReportResponseDTO.builder()
                .period(p)
                .year(year)
                .month(month)
                .quarter(quarter)
                .periodLabel(periodLabel)
                .dateRange(FinancialReportResponseDTO.DateRangeDTO.builder()
                        .startDate(start.format(ISO))
                        .endDate(end.format(ISO))
                        .build())
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netDifference(netDifference)
                .selectedCategoryIds(new ArrayList<>(selectedCategorySet))
                .availableCategories(buildAvailableCategories())
                .breakdown(breakdown)
                .build();
    }

    /**
     * Selalu sama dengan {@link IncomeCategory} + {@link ExpenseCategory} (sumber kebenaran tunggal).
     */
    private static List<FinancialReportResponseDTO.CategoryOptionDTO> buildAvailableCategories() {
        List<FinancialReportResponseDTO.CategoryOptionDTO> options = new ArrayList<>();
        for (IncomeCategory c : IncomeCategory.values()) {
            options.add(FinancialReportResponseDTO.CategoryOptionDTO.builder()
                    .id(c.name())
                    .label(incomeCategoryPickerLabel(c))
                    .build());
        }
        for (ExpenseCategory c : ExpenseCategory.values()) {
            options.add(FinancialReportResponseDTO.CategoryOptionDTO.builder()
                    .id(c.name())
                    .label(formatExpenseCategory(c))
                    .build());
        }
        return options;
    }

    /** Label filter: selaras dengan form laporan masuk (IncomeTransaction*). */
    private static String incomeCategoryPickerLabel(IncomeCategory c) {
        return switch (c) {
            case DONASI -> "Donasi";
            case ZAKAT -> "Zakat";
            case INFAQ -> "Infaq";
            case LAIN_LAIN -> "Lain-lain";
        };
    }

    private static String formatIncomeCategory(IncomeCategory c) {
        return switch (c) {
            case DONASI -> "Donasi";
            case ZAKAT -> "Zakat";
            case INFAQ -> "Infaq";
            case LAIN_LAIN -> "Lain-lain (pemasukan)";
        };
    }

    private static String formatExpenseCategory(ExpenseCategory c) {
        return switch (c) {
            case OPERASIONAL -> "Operasional";
            case GAJI_HONOR -> "Gaji & honor";
            case PROGRAM -> "Program";
            case UTILITAS -> "Utilitas";
            case PEMELIHARAAN -> "Pemeliharaan";
            case TRANSPORTASI -> "Transportasi";
        };
    }
}
