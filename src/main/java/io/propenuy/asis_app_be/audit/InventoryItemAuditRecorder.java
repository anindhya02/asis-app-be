package io.propenuy.asis_app_be.audit;

import io.propenuy.asis_app_be.model.InventoryItem;
import io.propenuy.asis_app_be.model.enums.AuditActionType;
import io.propenuy.asis_app_be.model.enums.AuditModuleCode;
import org.springframework.stereotype.Service;

@Service
public class InventoryItemAuditRecorder {

    private final AuditLogWriter auditLogWriter;
    private final InventoryItemAuditSnapshot snapshot;

    public InventoryItemAuditRecorder(AuditLogWriter auditLogWriter, InventoryItemAuditSnapshot snapshot) {
        this.auditLogWriter = auditLogWriter;
        this.snapshot = snapshot;
    }

    public void recordAfterCreate(InventoryItem item) {
        if (item == null || item.getId() == null) {
            return;
        }
        auditLogWriter.persist(
                AuditModuleCode.INVENTORY_ITEM,
                InventoryItem.class,
                item.getId(),
                AuditActionType.CREATE,
                null,
                snapshot.toJson(item));
    }
}
