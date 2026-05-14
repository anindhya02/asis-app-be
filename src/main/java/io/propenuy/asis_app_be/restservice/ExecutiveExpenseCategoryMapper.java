package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.model.enums.ExpenseCategory;

/**
 * Pemetaan tunggal (backend) untuk dashboard eksekutif: biaya program vs operasional.
 * <p>
 * Sumber kebenaran kategori transaksi adalah {@link ExpenseCategory}. Tidak ada flag
 * terpisah di entitas; pemetaan ke bucket EIS dilakukan di sini agar mudah dirawat.
 * <ul>
 *   <li><b>PROGRAM</b> — hanya {@link ExpenseCategory#PROGRAM}.</li>
 *   <li><b>OPERATIONAL</b> — semua nilai enum lain (operasional, gaji &amp; honor, utilitas,
 *   pemeliharaan, transportasi, dll.) dianggap beban operasional/non-program untuk rasio ini.</li>
 * </ul>
 */
public final class ExecutiveExpenseCategoryMapper {

    private ExecutiveExpenseCategoryMapper() {
    }

    public static boolean isProgramExpense(ExpenseCategory category) {
        return category == ExpenseCategory.PROGRAM;
    }

    public static boolean isOperationalExpense(ExpenseCategory category) {
        return !isProgramExpense(category);
    }
}
