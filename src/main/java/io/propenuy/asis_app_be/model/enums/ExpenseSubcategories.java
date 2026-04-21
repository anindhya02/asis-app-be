package io.propenuy.asis_app_be.model.enums;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class ExpenseSubcategories {

    private static final Map<ExpenseCategory, Set<String>> BY_CATEGORY = new EnumMap<>(ExpenseCategory.class);

    static {
        BY_CATEGORY.put(ExpenseCategory.OPERASIONAL, Set.of(
                "ALAT_TULIS_KANTOR", "PERLENGKAPAN", "KONSUMSI", "CETAK_FOTOKOPI"
        ));
        BY_CATEGORY.put(ExpenseCategory.GAJI_HONOR, Set.of(
                "GAJI_TETAP", "HONOR_KEGIATAN", "INSENTIF", "THR"
        ));
        BY_CATEGORY.put(ExpenseCategory.PROGRAM, Set.of(
                "PENDIDIKAN", "SOSIAL", "KESEHATAN", "DAKWAH", "PEMBANGUNAN"
        ));
        BY_CATEGORY.put(ExpenseCategory.UTILITAS, Set.of(
                "LISTRIK", "AIR", "INTERNET", "TELEPON"
        ));
        BY_CATEGORY.put(ExpenseCategory.PEMELIHARAAN, Set.of(
                "GEDUNG", "KENDARAAN", "PERALATAN", "TAMAN"
        ));
        BY_CATEGORY.put(ExpenseCategory.TRANSPORTASI, Set.of(
                "BBM", "SEWA_KENDARAAN", "TOL_PARKIR", "TIKET_PERJALANAN"
        ));
    }

    private ExpenseSubcategories() {
    }

    public static boolean isAllowed(ExpenseCategory category, String subCategory) {
        if (subCategory == null || subCategory.isBlank()) {
            return false;
        }
        Set<String> allowed = BY_CATEGORY.getOrDefault(category, Collections.emptySet());
        return allowed.contains(subCategory.trim());
    }

    public static String allowedListHint(ExpenseCategory category) {
        Set<String> allowed = BY_CATEGORY.get(category);
        if (allowed == null || allowed.isEmpty()) {
            return "";
        }
        return String.join(", ", allowed);
    }
}
