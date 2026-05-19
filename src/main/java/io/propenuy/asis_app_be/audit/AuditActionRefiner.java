package io.propenuy.asis_app_be.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.propenuy.asis_app_be.model.enums.AuditActionType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Menyempurnakan jenis aksi audit: soft delete (deletedAt / status INACTIVE) → DELETE,
 * serta transisi status {@code PaymentRequest} → APPROVE / REJECT / REVIEW / CANCEL.
 */
@Component
public class AuditActionRefiner {

    private final ObjectMapper objectMapper;

    public AuditActionRefiner(@Qualifier("auditObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @param base         CREATE atau UPDATE dari operasi repository
     * @param newValueJson JSON setelah save; di-set null jika soft delete agar tampilan "nilai baru" kosong
     */
    public Refinement refine(Class<?> entityClass, AuditActionType base, String oldJson, String newValueJson) {
        if (base == AuditActionType.CREATE || base == AuditActionType.DELETE) {
            return new Refinement(base, newValueJson);
        }
        if (oldJson == null || newValueJson == null) {
            return new Refinement(base, newValueJson);
        }
        try {
            JsonNode old = objectMapper.readTree(oldJson);
            JsonNode neu = objectMapper.readTree(newValueJson);
            String simple = entityClass.getSimpleName();

            if ("PaymentRequest".equals(simple)) {
                AuditActionType pr = refinePaymentRequest(old, neu);
                if (pr != null) {
                    return new Refinement(pr, newValueJson);
                }
            }

            if (base == AuditActionType.UPDATE && isSoftDeleteTransition(old, neu, simple)) {
                return new Refinement(AuditActionType.DELETE, null);
            }
        } catch (Exception ignored) {
            // biarkan nilai default
        }
        return new Refinement(base, newValueJson);
    }

    private AuditActionType refinePaymentRequest(JsonNode old, JsonNode neu) {
        String oldStatus = text(old, "status");
        String newStatus = text(neu, "status");
        if (newStatus == null || newStatus.equals(oldStatus)) {
            return null;
        }
        return switch (newStatus) {
            case "APPROVED" -> AuditActionType.APPROVE;
            case "REJECTED" -> AuditActionType.REJECT;
            case "REVISION_REQUESTED" -> AuditActionType.REVIEW;
            case "CANCELLED" -> AuditActionType.CANCEL;
            default -> null;
        };
    }

    private static boolean isSoftDeleteTransition(JsonNode old, JsonNode neu, String entitySimple) {
        if ("Activity".equals(entitySimple)) {
            return deletedAtJustSet(old, neu);
        }
        if ("ExpenseTransaction".equals(entitySimple)
                || "IncomeTransaction".equals(entitySimple)
                || "User".equals(entitySimple)) {
            if (deletedAtJustSet(old, neu)) {
                return true;
            }
            return inactiveStatusTransition(old, neu);
        }
        return false;
    }

    private static boolean deletedAtJustSet(JsonNode old, JsonNode neu) {
        if (neu == null || !neu.has("deletedAt") || neu.get("deletedAt").isNull()) {
            return false;
        }
        JsonNode nDel = neu.get("deletedAt");
        if (!nDel.isValueNode()) {
            return false;
        }
        String newVal = nDel.asText();
        if (newVal == null || newVal.isBlank()) {
            return false;
        }
        if (old == null || !old.has("deletedAt") || old.get("deletedAt").isNull()) {
            return true;
        }
        String oldVal = old.get("deletedAt").asText();
        return oldVal == null || oldVal.isBlank();
    }

    /** Status menjadi INACTIVE dari bukan-INACTIVE (kas masuk/keluar, user, dll.) */
    private static boolean inactiveStatusTransition(JsonNode old, JsonNode neu) {
        String ns = text(neu, "status");
        if (ns == null || !"INACTIVE".equalsIgnoreCase(ns)) {
            return false;
        }
        String os = text(old, "status");
        return os == null || !"INACTIVE".equalsIgnoreCase(os);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        JsonNode v = node.get(field);
        if (v.isTextual()) {
            return v.asText();
        }
        if (v.isNumber() || v.isBoolean()) {
            return v.asText();
        }
        return v.toString();
    }

    public record Refinement(AuditActionType actionType, String newValueJson) {
    }
}
