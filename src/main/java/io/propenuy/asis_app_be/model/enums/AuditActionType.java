package io.propenuy.asis_app_be.model.enums;

import java.util.Optional;

public enum AuditActionType {
    CREATE,
    UPDATE,
    DELETE,
    /** Persetujuan pengajuan dana */
    APPROVE,
    /** Penolakan pengajuan dana */
    REJECT,
    /** Permintaan revisi / review pengajuan dana */
    REVIEW,
    /** Pembatalan pengajuan dana */
    CANCEL,
    /** Pengajuan dana diajukan untuk review */
    SUBMIT;

    public static Optional<AuditActionType> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(AuditActionType.valueOf(raw.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
