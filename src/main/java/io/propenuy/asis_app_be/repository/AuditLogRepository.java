package io.propenuy.asis_app_be.repository;

import io.propenuy.asis_app_be.model.AuditLog;
import io.propenuy.asis_app_be.model.enums.AuditActionType;
import io.propenuy.asis_app_be.model.enums.AuditModuleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    boolean existsByModuleCodeAndEntityIdAndActionType(
            AuditModuleCode moduleCode,
            String entityId,
            AuditActionType actionType);
}
