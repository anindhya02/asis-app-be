package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.model.PaymentRequest;
import io.propenuy.asis_app_be.model.enums.ExpenseCategory;
import io.propenuy.asis_app_be.model.enums.PaymentRequestStatus;
import io.propenuy.asis_app_be.repository.PaymentRequestRepository;
import io.propenuy.asis_app_be.restdto.response.PaymentRequestListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.PaymentRequestResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentRequestRestServiceImpl implements PaymentRequestRestService {

    private final PaymentRequestRepository paymentRequestRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    @Transactional(readOnly = true)
    public PaymentRequestListResponseDTO list(
            String startDateStr,
            String endDateStr,
            String statusStr,
            String expenseCategoryStr,
            String search,
            int page,
            int size,
            String currentUsername
    ) {
        final LocalDate startDate;
        final LocalDate endDate;

        if (startDateStr != null && !startDateStr.isBlank()) {
            try {
                startDate = LocalDate.parse(startDateStr.trim(), DATE_FORMATTER);
            } catch (Exception e) {
                throw new IllegalArgumentException("Format startDate tidak valid. Gunakan format YYYY-MM-DD");
            }
        } else {
            startDate = null;
        }

        if (endDateStr != null && !endDateStr.isBlank()) {
            try {
                endDate = LocalDate.parse(endDateStr.trim(), DATE_FORMATTER);
            } catch (Exception e) {
                throw new IllegalArgumentException("Format endDate tidak valid. Gunakan format YYYY-MM-DD");
            }
        } else {
            endDate = null;
        }

        final PaymentRequestStatus status;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = PaymentRequestStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Status tidak valid. Nilai yang diterima: DRAFT, PENDING_REVIEW, REVISION_REQUESTED, APPROVED, REJECTED, CANCELLED"
                );
            }
        } else {
            status = null;
        }

        final ExpenseCategory expenseCategory;
        if (expenseCategoryStr != null && !expenseCategoryStr.isBlank()) {
            try {
                String catUpper = expenseCategoryStr.toUpperCase().replace("-", "_").replace(" ", "_");
                expenseCategory = ExpenseCategory.valueOf(catUpper);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Kategori tidak valid. Nilai yang diterima: OPERASIONAL, KONSUMSI, TRANSPORTASI, PERLENGKAPAN, PROGRAM_KEGIATAN, GAJI, INFRASTRUKTUR, LAIN_LAIN"
                );
            }
        } else {
            expenseCategory = null;
        }

        final String searchTerm = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size > 0 ? size : 10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Specification<PaymentRequest> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            // Pengurus can only see their own tickets
            predicates.add(cb.equal(root.get("createdBy").get("username"), currentUsername));

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(LocalTime.MAX)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (expenseCategory != null) {
                predicates.add(cb.equal(root.get("expenseCategory"), expenseCategory));
            }
            if (searchTerm != null) {
                jakarta.persistence.criteria.Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("title")), "%" + searchTerm + "%"),
                        cb.like(cb.lower(root.get("purpose")), "%" + searchTerm + "%")
                );
                predicates.add(searchPredicate);
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<PaymentRequest> pageResult = paymentRequestRepository.findAll(spec, pageable);

        List<PaymentRequestResponseDTO> content = pageResult.getContent().stream()
                .map(this::toResponseDTO)
                .toList();

        return PaymentRequestListResponseDTO.builder()
                .content(content)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
    }

    private PaymentRequestResponseDTO toResponseDTO(PaymentRequest pr) {
        return PaymentRequestResponseDTO.builder()
                .id(pr.getId())
                .title(pr.getTitle())
                .purpose(pr.getPurpose())
                .amount(pr.getAmount())
                .expenseCategory(pr.getExpenseCategory().name())
                .program(pr.getProgram())
                .status(pr.getStatus().name())
                .createdByUsername(pr.getCreatedBy().getUsername())
                .createdAt(pr.getCreatedAt())
                .updatedAt(pr.getUpdatedAt())
                .build();
    }
}
