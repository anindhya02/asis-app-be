package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.model.enums.ExpenseCategory;

public final class ExpenseCategoryRules {

    private ExpenseCategoryRules() {
    }

    public static String normalizeSubCategory(String subCategory) {
        if (subCategory == null) {
            return null;
        }

        String normalized = subCategory.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public static void validateSubForMain(ExpenseCategory mainCategory, String subCategory) {
        if (mainCategory == null) {
            throw new IllegalArgumentException("Kategori utama wajib diisi");
        }

        // Placeholder validation to keep current API behavior:
        // subCategory is optional and accepted for all main categories.
        if (subCategory != null && subCategory.length() > 255) {
            throw new IllegalArgumentException("Sub kategori maksimal 255 karakter");
        }
    }
}
