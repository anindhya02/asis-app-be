package io.propenuy.asis_app_be.audit;

import io.propenuy.asis_app_be.model.AuditLog;
import io.propenuy.asis_app_be.model.Activity;
import io.propenuy.asis_app_be.model.ActivityAttachment;
import io.propenuy.asis_app_be.model.ExpenseTransaction;
import io.propenuy.asis_app_be.model.IncomeTransaction;
import io.propenuy.asis_app_be.model.InventoryItem;
import io.propenuy.asis_app_be.model.InventoryItemBreakdown;
import io.propenuy.asis_app_be.model.InventoryUsageLog;
import io.propenuy.asis_app_be.model.PaymentRequest;
import io.propenuy.asis_app_be.model.PaymentRequestBreakdown;
import io.propenuy.asis_app_be.model.PaymentRequestReviewActivity;
import io.propenuy.asis_app_be.model.User;
import io.propenuy.asis_app_be.model.enums.AuditActionType;
import io.propenuy.asis_app_be.model.enums.AuditModuleCode;
import io.propenuy.asis_app_be.repository.AuditLogRepository;
import io.propenuy.asis_app_be.repository.UserRepository;
import jakarta.persistence.Id;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Aspect
@Component
public class JpaRepositoryAuditAspect {

    private static final Logger log = LoggerFactory.getLogger(JpaRepositoryAuditAspect.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AuditEntitySerializer auditEntitySerializer;
    private final AuditActionRefiner auditActionRefiner;
    private final AuditOldSnapshotService auditOldSnapshotService;

    public JpaRepositoryAuditAspect(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            AuditEntitySerializer auditEntitySerializer,
            AuditActionRefiner auditActionRefiner,
            AuditOldSnapshotService auditOldSnapshotService) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.auditEntitySerializer = auditEntitySerializer;
        this.auditActionRefiner = auditActionRefiner;
        this.auditOldSnapshotService = auditOldSnapshotService;
    }

    @Around("execution(* org.springframework.data.repository.CrudRepository+.save(..))")
    public Object aroundSave(ProceedingJoinPoint pjp) throws Throwable {
        if (!isAppRepository(pjp.getTarget())) {
            return pjp.proceed();
        }
        Object arg = pjp.getArgs()[0];
        if (arg == null) {
            return pjp.proceed();
        }
        if (isAuditLogEntity(arg)) {
            return pjp.proceed();
        }
        Class<?> entityClass = Hibernate.getClass(arg);
        Serializable id = extractId(arg);
        String oldJson = auditOldSnapshotService.loadSnapshotOrNull(entityClass, id);
        Object result = pjp.proceed();
        Object persisted = result != null ? result : arg;
        // Diaudit eksplisit di service layer (snapshot sebelum mutasi).
        if (persisted instanceof User
                || persisted instanceof IncomeTransaction
                || persisted instanceof ExpenseTransaction
                || persisted instanceof PaymentRequest
                || persisted instanceof PaymentRequestBreakdown
                || persisted instanceof PaymentRequestReviewActivity
                || persisted instanceof Activity
                || persisted instanceof ActivityAttachment
                || persisted instanceof InventoryItem
                || persisted instanceof InventoryItemBreakdown
                || persisted instanceof InventoryUsageLog) {
            return result;
        }
        String newJson = auditEntitySerializer.toJson(persisted);
        AuditActionType base = oldJson == null ? AuditActionType.CREATE : AuditActionType.UPDATE;
        AuditActionRefiner.Refinement r =
                auditActionRefiner.refine(Hibernate.getClass(persisted), base, oldJson, newJson);
        writeAuditLog(persisted, r.actionType(), oldJson, r.newValueJson());
        return result;
    }

    @Around("execution(* org.springframework.data.repository.ListCrudRepository+.saveAll(..))")
    public Object aroundSaveAll(ProceedingJoinPoint pjp) throws Throwable {
        if (!isAppRepository(pjp.getTarget())) {
            return pjp.proceed();
        }
        Iterable<?> iterable = (Iterable<?>) pjp.getArgs()[0];
        List<Object> entities = new ArrayList<>();
        iterable.forEach(entities::add);
        List<String> oldJsonList = new ArrayList<>(entities.size());
        for (Object entity : entities) {
            if (entity == null || isAuditLogEntity(entity)) {
                oldJsonList.add(null);
                continue;
            }
            oldJsonList.add(
                    auditOldSnapshotService.loadSnapshotOrNull(Hibernate.getClass(entity), extractId(entity)));
        }
        Object result = pjp.proceed();
        if (!(result instanceof List<?> savedList)) {
            return result;
        }
        for (int i = 0; i < savedList.size(); i++) {
            Object persisted = savedList.get(i);
            if (persisted == null || isAuditLogEntity(persisted)) {
                continue;
            }
            String oldJ = i < oldJsonList.size() ? oldJsonList.get(i) : null;
            String newJ = auditEntitySerializer.toJson(persisted);
            AuditActionType base = oldJ == null ? AuditActionType.CREATE : AuditActionType.UPDATE;
            AuditActionRefiner.Refinement r =
                    auditActionRefiner.refine(Hibernate.getClass(persisted), base, oldJ, newJ);
            writeAuditLog(persisted, r.actionType(), oldJ, r.newValueJson());
        }
        return result;
    }

    @Around("execution(* org.springframework.data.repository.CrudRepository+.delete(..))")
    public Object aroundDelete(ProceedingJoinPoint pjp) throws Throwable {
        if (!isAppRepository(pjp.getTarget())) {
            return pjp.proceed();
        }
        Object entity = pjp.getArgs()[0];
        if (entity == null || isAuditLogEntity(entity)) {
            return pjp.proceed();
        }
        if (entity instanceof ActivityAttachment) {
            return pjp.proceed();
        }
        Class<?> entityClass = Hibernate.getClass(entity);
        Serializable id = extractId(entity);
        String snap = id == null ? null : auditOldSnapshotService.loadSnapshotOrNull(entityClass, id);
        String oldJson = snap != null ? snap : auditEntitySerializer.toJson(entity);
        Object proceed = pjp.proceed();
        writeAuditLog(entity, AuditActionType.DELETE, oldJson, null);
        return proceed;
    }

    @Around("execution(* org.springframework.data.repository.CrudRepository+.deleteById(..))")
    public Object aroundDeleteById(ProceedingJoinPoint pjp) throws Throwable {
        if (!isAppRepository(pjp.getTarget())) {
            return pjp.proceed();
        }
        Serializable id = (Serializable) pjp.getArgs()[0];
        Class<?> domainClass = AuditOldSnapshotService.resolveDomainClass(pjp.getTarget());
        String oldJson = null;
        if (domainClass != null && id != null) {
            oldJson = auditOldSnapshotService.loadSnapshotOrNull(domainClass, id);
        }
        Object proceed = pjp.proceed();
        if (oldJson != null && domainClass != null && id != null && !AuditLog.class.equals(domainClass)) {
            persistAuditRow(domainClass, id, AuditActionType.DELETE, oldJson, null);
        }
        return proceed;
    }

    private boolean isAppRepository(Object target) {
        if (target == null) {
            return false;
        }
        for (Class<?> ifc : target.getClass().getInterfaces()) {
            if (ifc.getName().startsWith("io.propenuy.asis_app_be.repository.")) {
                return true;
            }
        }
        return false;
    }

    private boolean isAuditLogEntity(Object entity) {
        Class<?> c = Hibernate.getClass(entity);
        return AuditLog.class.equals(c);
    }

    private void writeAuditLog(Object entity, AuditActionType actionType, String oldJson, String newJson) {
        Class<?> clazz = Hibernate.getClass(entity);
        Serializable id = extractId(entity);
        persistAuditRow(clazz, id, actionType, oldJson, newJson);
    }

    private void persistAuditRow(Class<?> clazz, Serializable id, AuditActionType actionType, String oldJson, String newJson) {
        try {
            AuditModuleCode module = AuditModuleCode.fromEntityClass(clazz);
            Actor actor = resolveActor();
            AuditLog row = AuditLog.builder()
                    .occurredAt(LocalDateTime.now())
                    .actionType(actionType)
                    .moduleCode(module)
                    .entityClassName(clazz.getSimpleName())
                    .entityId(id != null ? String.valueOf(id) : null)
                    .oldValueJson(oldJson)
                    .newValueJson(newJson)
                    .actorUserId(actor.userId())
                    .actorUsername(actor.username())
                    .actorRole(actor.role())
                    .actorNama(actor.nama())
                    .build();
            auditLogRepository.save(row);
        } catch (Exception e) {
            log.warn("Failed to persist audit log: {}", e.getMessage());
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

    private Serializable extractId(Object entity) {
        if (entity == null) {
            return null;
        }
        Class<?> clazz = Hibernate.getClass(entity);
        AtomicReference<Object> holder = new AtomicReference<>();
        for (Class<?> c = clazz; c != null && c != Object.class && holder.get() == null; c = c.getSuperclass()) {
            ReflectionUtils.doWithFields(c, field -> {
                if (field.getAnnotation(Id.class) != null && holder.get() == null) {
                    ReflectionUtils.makeAccessible(field);
                    holder.set(field.get(entity));
                }
            });
        }
        Object id = holder.get();
        return id instanceof Serializable s ? s : null;
    }

    private record Actor(UUID userId, String username, String role, String nama) {
    }
}
