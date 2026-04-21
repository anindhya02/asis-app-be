package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.model.enums.ExpenseCategory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class ExpenseCategoryRules {

    private static final Map<ExpenseCategory, Set<String>> ALLOWED_SUBS_BY_CATEGORY =
            new EnumMap<>(ExpenseCategory.class);

    static {
        ALLOWED_SUBS_BY_CATEGORY.put(ExpenseCategory.OPERASIONAL, Set.of(
                "ALAT_TULIS_KANTOR", "PERLENGKAPAN", "KONSUMSI", "CETAK_FOTOKOPI"
        ));
        ALLOWED_SUBS_BY_CATEGORY.put(ExpenseCategory.GAJI_HONOR, Set.of(
                "GAJI_TETAP", "HONOR_KEGIATAN", "INSENTIF", "THR"
        ));
        ALLOWED_SUBS_BY_CATEGORY.put(ExpenseCategory.PROGRAM, Set.of(
                "PENDIDIKAN", "SOSIAL", "KESEHATAN", "DAKWAH", "PEMBANGUNAN"
        ));
        ALLOWED_SUBS_BY_CATEGORY.put(ExpenseCategory.UTILITAS, Set.of(
                "LISTRIK", "AIR", "INTERNET", "TELEPON"
        ));
        ALLOWED_SUBS_BY_CATEGORY.put(ExpenseCategory.PEMELIHARAAN, Set.of(
                "GEDUNG", "KENDARAAN", "PERALATAN", "TAMAN"
        ));
        ALLOWED_SUBS_BY_CATEGORY.put(ExpenseCategory.TRANSPORTASI, Set.of(
                "BBM", "SEWA_KENDARAAN", "TOL_PARKIR", "TIKET_PERJALANAN"
        ));
    }

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

        Set<String> allowed = ALLOWED_SUBS_BY_CATEGORY.get(mainCategory);
        if (allowed == null || !allowed.contains(subCategory)) {
            throw new IllegalArgumentException(
                    "Sub kategori tidak valid untuk kategori " + mainCategory.name()
            );
        }
    }
}
