package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.model.enums.ExpenseCategory;
import io.propenuy.asis_app_be.model.enums.ExpenseSubcategories;

public final class ExpenseCategoryRules {

    private ExpenseCategoryRules() {
    }

    public static String normalizeSubCategory(String subCategory) {
        if (subCategory == null) {
            return null;
        }

        String trimmed = subCategory.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed
                .toUpperCase()
                .replace("&", " ")
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    public static void validateSubForMain(ExpenseCategory mainCategory, String subCategory) {
        if (mainCategory == null) {
            throw new IllegalArgumentException("Kategori utama wajib diisi");
        }

        if (subCategory == null) {
            return;
        }

        if (subCategory.length() > 255) {
            throw new IllegalArgumentException("Sub kategori maksimal 255 karakter");
        }

        if (!ExpenseSubcategories.isAllowedExact(mainCategory, subCategory)) {
            throw new IllegalArgumentException(
                    "Sub kategori tidak valid untuk kategori " + mainCategory.name()
            );
        }
    }
}
