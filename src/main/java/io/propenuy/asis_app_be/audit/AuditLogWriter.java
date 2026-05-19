package io.propenuy.asis_app_be.audit;

import io.propenuy.asis_app_be.model.AuditLog;
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

@Service
public class AuditLogWriter {

    private static final Logger log = LoggerFactory.getLogger(AuditLogWriter.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogWriter(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public void persist(
            AuditModuleCode moduleCode,
            Class<?> entityClass,
            UUID entityId,
            AuditActionType actionType,
            String oldValueJson,
            String newValueJson) {
        try {
            Actor actor = resolveActor();
            AuditLog row = AuditLog.builder()
                    .occurredAt(LocalDateTime.now())
                    .actionType(actionType)
                    .moduleCode(moduleCode)
                    .entityClassName(entityClass.getSimpleName())
                    .entityId(entityId != null ? String.valueOf(entityId) : null)
                    .oldValueJson(oldValueJson)
                    .newValueJson(newValueJson)
                    .actorUserId(actor.userId())
                    .actorUsername(actor.username())
                    .actorRole(actor.role())
                    .actorNama(actor.nama())
                    .build();
            auditLogRepository.save(row);
        } catch (Exception e) {
            log.warn("Failed to persist audit log ({}): {}", moduleCode, e.getMessage());
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
