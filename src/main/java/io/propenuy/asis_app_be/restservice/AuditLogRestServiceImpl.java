package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.model.AuditLog;
import io.propenuy.asis_app_be.model.enums.AuditActionType;
import io.propenuy.asis_app_be.model.enums.AuditModuleCode;
import io.propenuy.asis_app_be.repository.AuditLogRepository;
import io.propenuy.asis_app_be.restdto.response.AuditLogPageDTO;
import io.propenuy.asis_app_be.restdto.response.AuditLogRowDTO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuditLogRestServiceImpl implements AuditLogRestService {

    /** Modul yang ditampilkan di audit log (selaras dengan filter FE). */
    private static final Set<AuditModuleCode> LISTED_MODULES = EnumSet.of(
            AuditModuleCode.USER,
            AuditModuleCode.INCOME_TRANSACTION,
            AuditModuleCode.EXPENSE_TRANSACTION,
            AuditModuleCode.PAYMENT_REQUEST,
            AuditModuleCode.INVENTORY_ITEM,
            AuditModuleCode.ACTIVITY
    );

    private final AuditLogRepository auditLogRepository;

    @Override
    public AuditLogPageDTO getAuditLogs(
            LocalDate fromDate,
            LocalDate toDate,
            String actionType,
            String moduleCode,
            String userSearch,
            int page,
            int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));

        Specification<AuditLog> spec = buildSpec(fromDate, toDate, actionType, moduleCode, userSearch);
        Page<AuditLog> result = auditLogRepository.findAll(
                spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "occurredAt"))
        );

        return AuditLogPageDTO.builder()
                .content(result.getContent().stream().map(this::toRow).toList())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .number(result.getNumber())
                .size(result.getSize())
                .build();
    }

    private Specification<AuditLog> buildSpec(
            LocalDate fromDate,
            LocalDate toDate,
            String actionType,
            String moduleCode,
            String userSearch) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (fromDate != null) {
                LocalDateTime start = fromDate.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), start));
            }
            if (toDate != null) {
                LocalDateTime end = toDate.atTime(LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), end));
            }
            AuditActionType.parse(actionType).ifPresent(at -> predicates.add(cb.equal(root.get("actionType"), at)));
            if (moduleCode != null && !moduleCode.isBlank()) {
                AuditModuleCode.fromCode(moduleCode).ifPresent(mc -> predicates.add(cb.equal(root.get("moduleCode"), mc)));
            } else {
                predicates.add(root.get("moduleCode").in(LISTED_MODULES));
            }

            if (userSearch != null && !userSearch.isBlank()) {
                String pattern = "%" + userSearch.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("actorUsername")), pattern),
                        cb.like(cb.lower(root.get("actorNama")), pattern)
                ));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private AuditLogRowDTO toRow(AuditLog e) {
        return AuditLogRowDTO.builder()
                .id(e.getId() != null ? e.getId().toString() : null)
                .occurredAt(e.getOccurredAt())
                .actionType(e.getActionType() != null ? e.getActionType().name() : null)
                .moduleCode(e.getModuleCode() != null ? e.getModuleCode().getCode() : null)
                .moduleLabel(e.getModuleCode() != null ? e.getModuleCode().getLabelId() : null)
                .entityClassName(e.getEntityClassName())
                .entityId(e.getEntityId())
                .oldValueJson(e.getOldValueJson())
                .newValueJson(e.getNewValueJson())
                .actorUsername(e.getActorUsername())
                .actorNama(e.getActorNama())
                .actorRole(e.getActorRole())
                .build();
    }
}
