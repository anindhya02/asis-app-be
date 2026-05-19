package io.propenuy.asis_app_be.audit;

import io.propenuy.asis_app_be.model.AuditLog;
import io.propenuy.asis_app_be.model.User;
import io.propenuy.asis_app_be.model.enums.AuditActionType;
import io.propenuy.asis_app_be.model.enums.AuditModuleCode;
import io.propenuy.asis_app_be.repository.AuditLogRepository;
import io.propenuy.asis_app_be.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit User Management dengan snapshot sebelum mutasi (menghindari L1 cache Hibernate
 * yang sudah berisi nilai baru saat {@code save()} dipanggil).
 */
@Service
public class UserAuditRecorder {

    private static final Logger log = LoggerFactory.getLogger(UserAuditRecorder.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final UserAuditSnapshot userAuditSnapshot;
    private final AuditEntitySerializer auditEntitySerializer;
    private final AuditActionRefiner auditActionRefiner;

    public UserAuditRecorder(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            UserAuditSnapshot userAuditSnapshot,
            AuditEntitySerializer auditEntitySerializer,
            AuditActionRefiner auditActionRefiner) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.userAuditSnapshot = userAuditSnapshot;
        this.auditEntitySerializer = auditEntitySerializer;
        this.auditActionRefiner = auditActionRefiner;
    }

    public record BeforeState(
            UUID userId,
            String nama,
            String username,
            String role,
            String status,
            LocalDateTime deletedAt,
            String deletedBy) {
    }

    public BeforeState capture(User user) {
        return new BeforeState(
                user.getUserId(),
                user.getNama(),
                user.getUsername(),
                user.getRole(),
                user.getStatus(),
                user.getDeletedAt(),
                user.getDeletedBy());
    }

    public void recordAfterCreate(User user) {
        String storedNew = userAuditSnapshot.toJson(user, null);
        persist(user.getUserId(), AuditActionType.CREATE, null, storedNew);
    }

    public void recordAfterUpdate(BeforeState before, User after, boolean passwordChanged) {
        String oldFullJson = toFullJson(before);
        String newFullJson = auditEntitySerializer.toJson(after);
        AuditActionRefiner.Refinement r =
                auditActionRefiner.refine(User.class, AuditActionType.UPDATE, oldFullJson, newFullJson);

        String storedOld = userAuditSnapshot.toJson(before);
        String storedNew = userAuditSnapshot.toJson(after, passwordChanged);
        persist(after.getUserId(), r.actionType(), storedOld, storedNew);
    }

    public void recordAfterDeactivate(BeforeState before, User after) {
        String oldFullJson = toFullJson(before);
        String newFullJson = auditEntitySerializer.toJson(after);
        AuditActionRefiner.Refinement r =
                auditActionRefiner.refine(User.class, AuditActionType.UPDATE, oldFullJson, newFullJson);

        String storedOld = userAuditSnapshot.toJson(before);
        String storedNew = null;
        if (r.actionType() != AuditActionType.DELETE) {
            storedNew = userAuditSnapshot.toJson(after, null);
        }
        persist(after.getUserId(), r.actionType(), storedOld, storedNew);
    }

    private String toFullJson(BeforeState before) {
        User u = User.builder()
                .userId(before.userId())
                .nama(before.nama())
                .username(before.username())
                .role(before.role())
                .status(before.status())
                .deletedAt(before.deletedAt())
                .deletedBy(before.deletedBy())
                .build();
        return auditEntitySerializer.toJson(u);
    }

    private void persist(UUID userId, AuditActionType actionType, String oldJson, String newJson) {
        try {
            Actor actor = resolveActor();
            AuditLog row = AuditLog.builder()
                    .occurredAt(LocalDateTime.now())
                    .actionType(actionType)
                    .moduleCode(AuditModuleCode.USER)
                    .entityClassName(User.class.getSimpleName())
                    .entityId(userId != null ? String.valueOf(userId) : null)
                    .oldValueJson(oldJson)
                    .newValueJson(newJson)
                    .actorUserId(actor.userId())
                    .actorUsername(actor.username())
                    .actorRole(actor.role())
                    .actorNama(actor.nama())
                    .build();
            auditLogRepository.save(row);
        } catch (Exception e) {
            log.warn("Failed to persist user audit log: {}", e.getMessage());
        }
    }

    private Actor resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return new Actor(null, null, null, null);
        }
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .map(u -> new Actor(u.getUserId(), u.getUsername(), u.getRole(), u.getNama()))
                .orElse(new Actor(null, username, null, null));
    }

    private record Actor(UUID userId, String username, String role, String nama) {
    }
}
