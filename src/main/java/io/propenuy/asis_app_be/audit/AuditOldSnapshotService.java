package io.propenuy.asis_app_be.audit;

import io.propenuy.asis_app_be.model.AuditLog;
import io.propenuy.asis_app_be.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.UUID;

/**
 * Membaca snapshot entitas dari transaksi terpisah agar tidak detach/merusak
 * persistence context pada save agregat (mis. PaymentRequest + breakdowns).
 */
@Service
public class AuditOldSnapshotService {

    @PersistenceContext
    private EntityManager entityManager;

    private final AuditEntitySerializer auditEntitySerializer;

    public AuditOldSnapshotService(AuditEntitySerializer auditEntitySerializer) {
        this.auditEntitySerializer = auditEntitySerializer;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public String loadSnapshotOrNull(Class<?> entityClass, Serializable id) {
        if (entityClass == null || id == null) {
            return null;
        }
        if (User.class.isAssignableFrom(entityClass) && id instanceof UUID userId) {
            return loadFreshUserJson(userId);
        }
        Object found = entityManager.find(entityClass, id);
        if (found == null) {
            return null;
        }
        if (AuditLog.class.equals(Hibernate.getClass(found))) {
            return null;
        }
        return auditEntitySerializer.toJson(found);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public String loadUserPasswordHash(UUID userId) {
        if (userId == null) {
            return null;
        }
        User user = loadFreshUserEntity(userId);
        return user == null ? null : user.getPassword();
    }

    private String loadFreshUserJson(UUID userId) {
        User user = loadFreshUserEntity(userId);
        return user == null ? null : auditEntitySerializer.toJson(user);
    }

    /**
     * Entitas User yang sudah dimutasi di persistence context yang sama masih ter-cache;
     * detach lalu load ulang agar snapshot audit membaca nilai di database.
     */
    private User loadFreshUserEntity(UUID userId) {
        User cached = entityManager.find(User.class, userId);
        if (cached != null) {
            entityManager.detach(cached);
        }
        return entityManager.find(User.class, userId);
    }

    /**
     * Menentukan kelas domain dari proxy Spring Data (mis. PaymentRequestRepository).
     */
    public static Class<?> resolveDomainClass(Object repositoryTarget) {
        if (repositoryTarget == null) {
            return null;
        }
        for (Class<?> ifc : repositoryTarget.getClass().getInterfaces()) {
            for (Type t : ifc.getGenericInterfaces()) {
                if (t instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> raw
                        && JpaRepository.class.isAssignableFrom(raw)) {
                    Type[] at = pt.getActualTypeArguments();
                    if (at.length > 0 && at[0] instanceof Class<?> c) {
                        return c;
                    }
                }
            }
        }
        return null;
    }
}
