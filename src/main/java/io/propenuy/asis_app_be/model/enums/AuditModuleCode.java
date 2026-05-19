package io.propenuy.asis_app_be.model.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Stable module keys for filtering and display (Indonesian labels on API layer).
 */
public enum AuditModuleCode {
    USER("USER", "User Management"),
    INCOME_TRANSACTION("INCOME_TRANSACTION", "Transaksi Kas Masuk"),
    EXPENSE_TRANSACTION("EXPENSE_TRANSACTION", "Transaksi Kas Keluar"),
    PAYMENT_REQUEST("PAYMENT_REQUEST", "Pengajuan Dana"),
    PAYMENT_REQUEST_BREAKDOWN("PAYMENT_REQUEST_BREAKDOWN", "Rincian Pengajuan Dana"),
    PAYMENT_REQUEST_REVIEW_ACTIVITY("PAYMENT_REQUEST_REVIEW_ACTIVITY", "Review Pengajuan Dana"),
    INVENTORY_ITEM("INVENTORY_ITEM", "Inventory Donasi"),
    INVENTORY_ITEM_BREAKDOWN("INVENTORY_ITEM_BREAKDOWN", "Rincian Inventory"),
    ACTIVITY("ACTIVITY", "Postingan Kegiatan"),
    ACTIVITY_ATTACHMENT("ACTIVITY_ATTACHMENT", "Lampiran Kegiatan"),
    REPLY("REPLY", "Balasan Kegiatan"),
    UNKNOWN("UNKNOWN", "Lainnya");

    private final String code;
    private final String labelId;

    AuditModuleCode(String code, String labelId) {
        this.code = code;
        this.labelId = labelId;
    }

    public String getCode() {
        return code;
    }

    public String getLabelId() {
        return labelId;
    }

    public static AuditModuleCode fromEntityClass(Class<?> entityClass) {
        String simple = entityClass.getSimpleName();
        return switch (simple) {
            case "User" -> USER;
            case "IncomeTransaction" -> INCOME_TRANSACTION;
            case "ExpenseTransaction" -> EXPENSE_TRANSACTION;
            case "PaymentRequest" -> PAYMENT_REQUEST;
            case "PaymentRequestBreakdown" -> PAYMENT_REQUEST_BREAKDOWN;
            case "PaymentRequestReviewActivity" -> PAYMENT_REQUEST_REVIEW_ACTIVITY;
            case "InventoryItem" -> INVENTORY_ITEM;
            case "InventoryItemBreakdown" -> INVENTORY_ITEM_BREAKDOWN;
            case "Activity" -> ACTIVITY;
            case "ActivityAttachment" -> ACTIVITY_ATTACHMENT;
            case "Reply" -> REPLY;
            default -> UNKNOWN;
        };
    }

    public static Optional<AuditModuleCode> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(v -> v.code.equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}
