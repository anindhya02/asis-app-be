package io.propenuy.asis_app_be.model;

import io.propenuy.asis_app_be.model.enums.AuditActionType;
import io.propenuy.asis_app_be.model.enums.AuditModuleCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_occurred_at", columnList = "occurredAt"),
                @Index(name = "idx_audit_module_code", columnList = "moduleCode"),
                @Index(name = "idx_audit_action", columnList = "actionType"),
                @Index(name = "idx_audit_actor_username", columnList = "actorUsername")
        }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuditActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AuditModuleCode moduleCode;

    @Column(nullable = false, length = 128)
    private String entityClassName;

    @Column(length = 64)
    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String oldValueJson;

    @Column(columnDefinition = "TEXT")
    private String newValueJson;

    private UUID actorUserId;

    @Column(length = 128)
    private String actorUsername;

    @Column(length = 64)
    private String actorRole;

    @Column(length = 256)
    private String actorNama;
}
