package io.propenuy.asis_app_be.model.enums;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class ExpenseSubcategories {

    private static final Map<ExpenseCategory, Set<String>> BY_CATEGORY = new EnumMap<>(ExpenseCategory.class);

    static {
        BY_CATEGORY.put(ExpenseCategory.OPERASIONAL, Set.of(
                "Listrik", "Air", "Internet", "Telepon", "Lain-lain"
        ));
        BY_CATEGORY.put(ExpenseCategory.KONSUMSI, Set.of(
                "Makanan", "Minuman", "Snack", "Lain-lain"
        ));
        BY_CATEGORY.put(ExpenseCategory.TRANSPORTASI, Set.of(
                "BBM", "Tol", "Parkir", "Sewa Kendaraan", "Lain-lain"
        ));
        BY_CATEGORY.put(ExpenseCategory.PERLENGKAPAN, Set.of(
                "ATK", "Peralatan", "Lain-lain"
        ));
        BY_CATEGORY.put(ExpenseCategory.PROGRAM_KEGIATAN, Set.of(
                "Santunan", "Beasiswa", "Kegiatan Sosial", "Lain-lain"
        ));
        BY_CATEGORY.put(ExpenseCategory.GAJI, Set.of(
                "Gaji Pokok", "Honor", "Tunjangan", "Lain-lain"
        ));
        BY_CATEGORY.put(ExpenseCategory.INFRASTRUKTUR, Set.of(
                "Renovasi", "Pembangunan", "Perbaikan", "Lain-lain"
        ));
        BY_CATEGORY.put(ExpenseCategory.LAIN_LAIN, Set.of("Lain-lain"));
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
