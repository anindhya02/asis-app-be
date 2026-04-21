package io.propenuy.asis_app_be.restservice;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.propenuy.asis_app_be.model.PaymentRequest;
import io.propenuy.asis_app_be.model.PaymentRequestBreakdown;
import io.propenuy.asis_app_be.model.PaymentRequestReviewActivity;
import io.propenuy.asis_app_be.model.User;
import io.propenuy.asis_app_be.model.enums.ExpenseCategory;
import io.propenuy.asis_app_be.model.enums.ExpenseSubcategories;
import io.propenuy.asis_app_be.model.enums.PaymentMethod;
import io.propenuy.asis_app_be.model.enums.PaymentRequestStatus;
import io.propenuy.asis_app_be.repository.PaymentRequestRepository;
import io.propenuy.asis_app_be.repository.PaymentRequestReviewActivityRepository;
import io.propenuy.asis_app_be.repository.UserRepository;
import io.propenuy.asis_app_be.restdto.response.PaymentRequestDetailResponseDTO;
import io.propenuy.asis_app_be.restdto.response.PaymentRequestListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.PaymentRequestResponseDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentRequestRestServiceImpl implements PaymentRequestRestService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_KETUA_YAYASAN = "KETUA YAYASAN";
    private static final String ROLE_PENGURUS = "PENGURUS";

    private final PaymentRequestRepository paymentRequestRepository;
    private final PaymentRequestReviewActivityRepository paymentRequestReviewActivityRepository;
    private final UserRepository userRepository;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final ObjectMapper objectMapper;

    private static final String CLOUDINARY_FOLDER = "payment-request-docs";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final List<String> ALLOWED_DOC_TYPES = List.of(
            "application/pdf"
    );

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
                        "Kategori tidak valid. Nilai yang diterima: OPERASIONAL, GAJI_HONOR, PROGRAM, UTILITAS, PEMELIHARAAN, TRANSPORTASI"
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

        // Resolve the current user to check role
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        String role = currentUser.getRole() == null ? "" : currentUser.getRole().trim();
        final boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        final boolean isKetuaYayasan = "KETUA YAYASAN".equalsIgnoreCase(role);

        Specification<PaymentRequest> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            // Admin: all tickets. Ketua Yayasan: all tickets except DRAFT. Pengurus: own tickets only (incl. own drafts).
            if (!isAdmin && !isKetuaYayasan) {
                predicates.add(cb.equal(root.get("createdBy").get("username"), currentUsername));
            }
            if (isKetuaYayasan) {
                predicates.add(cb.notEqual(root.get("status"), PaymentRequestStatus.DRAFT));
            }

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
                        cb.like(cb.lower(cb.coalesce(root.get("notes"), "")), "%" + searchTerm + "%")
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

    @Override
    @Transactional
    public PaymentRequestResponseDTO create(
            String title,
            String neededDateStr,
            String expenseCategoryStr,
            String subCategory,
            String amountStr,
            String paymentMethodStr,
            String notes,
            String breakdownListJson,
            String submitStr,
            MultipartFile supportingDocument,
            String currentUsername
    ) {
        boolean isSubmit = "true".equalsIgnoreCase(submitStr);

        // Validation
        if (isSubmit) {
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("Judul wajib diisi");
            }
            if (neededDateStr == null || neededDateStr.isBlank()) {
                throw new IllegalArgumentException("Tanggal penggunaan dana wajib diisi");
            }
            if (expenseCategoryStr == null || expenseCategoryStr.isBlank()) {
                throw new IllegalArgumentException("Kategori pengeluaran wajib dipilih");
            }
            if (amountStr == null || amountStr.isBlank()) {
                throw new IllegalArgumentException("Nominal penggunaan dana wajib diisi");
            }
            if (subCategory == null || subCategory.isBlank()) {
                throw new IllegalArgumentException("Sub-kategori wajib diisi");
            }
            if (paymentMethodStr == null || paymentMethodStr.isBlank()) {
                throw new IllegalArgumentException("Metode pembayaran wajib dipilih");
            }
            if (supportingDocument == null || supportingDocument.isEmpty()) {
                throw new IllegalArgumentException("Dokumen pendukung wajib diunggah");
            }
        } else {
            if ((title == null || title.isBlank())
                    || (expenseCategoryStr == null || expenseCategoryStr.isBlank())
                    || (neededDateStr == null || neededDateStr.isBlank())) {
                throw new IllegalArgumentException("Minimal isi judul, tanggal penggunaan dana, dan kategori untuk menyimpan draft.");
            }
        }

        // Parse expenseCategory
        ExpenseCategory expenseCategory;
        if (expenseCategoryStr != null && !expenseCategoryStr.isBlank()) {
            try {
                String catUpper = expenseCategoryStr.toUpperCase().replace("-", "_").replace(" ", "_");
                expenseCategory = ExpenseCategory.valueOf(catUpper);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Kategori tidak valid. Nilai yang diterima: OPERASIONAL, GAJI_HONOR, PROGRAM, UTILITAS, PEMELIHARAAN, TRANSPORTASI"
                );
            }
        } else {
            expenseCategory = ExpenseCategory.OPERASIONAL;
        }

        validateSubCategoryIfPresent(expenseCategory, subCategory);

        // Parse neededDate (tanggal penggunaan dana)
        LocalDate neededDate = null;
        if (neededDateStr != null && !neededDateStr.isBlank()) {
            try {
                neededDate = LocalDate.parse(neededDateStr.trim(), DATE_FORMATTER);
            } catch (Exception e) {
                throw new IllegalArgumentException("Format tanggal tidak valid. Gunakan format YYYY-MM-DD");
            }
        }

        // Parse amount
        BigDecimal amount;
        if (amountStr != null && !amountStr.isBlank()) {
            try {
                amount = new BigDecimal(amountStr.trim().replace(",", "."));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Nominal penggunaan dana harus berupa angka");
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Nominal penggunaan dana harus lebih dari 0");
            }
        } else {
            amount = BigDecimal.ZERO;
        }

        // Parse paymentMethod
        PaymentMethod paymentMethod = null;
        if (paymentMethodStr != null && !paymentMethodStr.isBlank()) {
            try {
                paymentMethod = PaymentMethod.valueOf(paymentMethodStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Metode pembayaran tidak valid. Nilai yang diterima: CASH, TRANSFER");
            }
        }

        List<Map<String, String>> breakdownItems = parseAndValidateBreakdownList(breakdownListJson, isSubmit, amount);

        // Validate supporting document file type if provided
        String documentUrl = null;
        String documentName = null;
        if (supportingDocument != null && !supportingDocument.isEmpty()) {
            String contentType = supportingDocument.getContentType();
            if (contentType == null ||
                    (!ALLOWED_IMAGE_TYPES.contains(contentType) && !ALLOWED_DOC_TYPES.contains(contentType))) {
                throw new IllegalArgumentException("Format file tidak didukung. Gunakan JPG, PNG, GIF, WebP, atau PDF (maks. 5MB)");
            }
            if (supportingDocument.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("Ukuran file melebihi batas 5MB");
            }

            try {
                String originalFilename = supportingDocument.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf("."))
                        : "";
                String storagePath = UUID.randomUUID() + extension;
                String resourceType = ALLOWED_DOC_TYPES.contains(contentType) ? "raw" : "image";
                documentUrl = cloudinaryStorageService.uploadFile(
                        supportingDocument, storagePath, CLOUDINARY_FOLDER, resourceType
                );
                documentName = originalFilename;
            } catch (IOException e) {
                throw new RuntimeException("Gagal mengupload dokumen pendukung: " + e.getMessage());
            }
        }

        // Get current user
        User createdByUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        // Build entity
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .title(title != null ? title.trim() : "")
                .amount(amount)
                .expenseCategory(expenseCategory)
                .subCategory(subCategory != null && !subCategory.isBlank() ? subCategory.trim() : null)
                .neededDate(neededDate)
                .paymentMethod(paymentMethod)
                .notes(notes != null && !notes.isBlank() ? notes.trim() : null)
                .supportingDocumentUrl(documentUrl)
                .supportingDocumentName(documentName)
                .status(isSubmit ? PaymentRequestStatus.PENDING_REVIEW : PaymentRequestStatus.DRAFT)
                .createdBy(createdByUser)
                .updatedBy(createdByUser)
                .build();

        paymentRequestRepository.save(paymentRequest);

        // Save breakdown items
        List<PaymentRequestBreakdown> breakdowns = new ArrayList<>();
        for (Map<String, String> item : breakdownItems) {
            PaymentRequestBreakdown breakdown = PaymentRequestBreakdown.builder()
                    .paymentRequest(paymentRequest)
                    .description(item.get("description").trim())
                    .amount(new BigDecimal(item.get("amount").trim().replace(",", ".")))
                    .build();
            breakdowns.add(breakdown);
        }
        if (!breakdowns.isEmpty()) {
            paymentRequest.getBreakdowns().addAll(breakdowns);
            paymentRequestRepository.save(paymentRequest);
        }

        if (isSubmit) {
            paymentRequestReviewActivityRepository.save(PaymentRequestReviewActivity.builder()
                    .paymentRequest(paymentRequest)
                    .actor(createdByUser)
                    .status(PaymentRequestStatus.PENDING_REVIEW)
                    .note(null)
                    .build());
        }

        return toResponseDTO(paymentRequest);
    }

    @Override
    @Transactional
    public PaymentRequestResponseDTO update(
            String id,
            String title,
            String neededDateStr,
            String expenseCategoryStr,
            String subCategory,
            String amountStr,
            String paymentMethodStr,
            String notes,
            String breakdownListJson,
            String submitStr,
            MultipartFile supportingDocument,
            String currentUsername
    ) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Format ID tidak valid");
        }

        PaymentRequest pr = paymentRequestRepository.findDetailById(uuid).orElse(null);
        if (pr == null) {
            return null;
        }

        User actor = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        if (pr.getCreatedBy() == null
                || pr.getCreatedBy().getUsername() == null
                || !pr.getCreatedBy().getUsername().equalsIgnoreCase(actor.getUsername())) {
            throw new PaymentRequestAccessForbiddenException("Tidak memiliki akses");
        }

        if (pr.getStatus() != PaymentRequestStatus.DRAFT
                && pr.getStatus() != PaymentRequestStatus.REVISION_REQUESTED) {
            throw new PaymentRequestAccessForbiddenException(
                    "Ticket hanya dapat diubah saat status Draft atau Revisi"
            );
        }

        boolean isSubmit = "true".equalsIgnoreCase(submitStr);

        if (isSubmit) {
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("Judul pengajuan wajib diisi");
            }
            if (neededDateStr == null || neededDateStr.isBlank()) {
                throw new IllegalArgumentException("Tanggal penggunaan dana wajib diisi");
            }
            if (expenseCategoryStr == null || expenseCategoryStr.isBlank()) {
                throw new IllegalArgumentException("Kategori pengeluaran wajib dipilih");
            }
            if (amountStr == null || amountStr.isBlank()) {
                throw new IllegalArgumentException("Nominal penggunaan dana wajib diisi");
            }
            if (subCategory == null || subCategory.isBlank()) {
                throw new IllegalArgumentException("Sub-kategori wajib diisi");
            }
            if (paymentMethodStr == null || paymentMethodStr.isBlank()) {
                throw new IllegalArgumentException("Metode pembayaran wajib dipilih");
            }
            boolean hasNewDoc = supportingDocument != null && !supportingDocument.isEmpty();
            boolean hasExistingDoc = pr.getSupportingDocumentUrl() != null && !pr.getSupportingDocumentUrl().isBlank();
            if (!hasNewDoc && !hasExistingDoc) {
                throw new IllegalArgumentException("Dokumen pendukung wajib diunggah");
            }
        } else {
            if ((title == null || title.isBlank())
                    || (expenseCategoryStr == null || expenseCategoryStr.isBlank())
                    || (neededDateStr == null || neededDateStr.isBlank())) {
                throw new IllegalArgumentException("Minimal isi judul, tanggal penggunaan dana, dan kategori untuk menyimpan draft.");
            }
        }

        ExpenseCategory expenseCategory;
        if (expenseCategoryStr != null && !expenseCategoryStr.isBlank()) {
            try {
                String catUpper = expenseCategoryStr.toUpperCase().replace("-", "_").replace(" ", "_");
                expenseCategory = ExpenseCategory.valueOf(catUpper);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Kategori tidak valid. Nilai yang diterima: OPERASIONAL, GAJI_HONOR, PROGRAM, UTILITAS, PEMELIHARAAN, TRANSPORTASI"
                );
            }
        } else {
            expenseCategory = ExpenseCategory.OPERASIONAL;
        }

        validateSubCategoryIfPresent(expenseCategory, subCategory);

        LocalDate neededDate = null;
        if (neededDateStr != null && !neededDateStr.isBlank()) {
            try {
                neededDate = LocalDate.parse(neededDateStr.trim(), DATE_FORMATTER);
            } catch (Exception e) {
                throw new IllegalArgumentException("Format tanggal tidak valid. Gunakan format YYYY-MM-DD");
            }
        }

        BigDecimal amount;
        if (amountStr != null && !amountStr.isBlank()) {
            try {
                amount = new BigDecimal(amountStr.trim().replace(",", "."));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Nominal penggunaan dana harus berupa angka");
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Nominal penggunaan dana harus lebih dari 0");
            }
        } else {
            amount = BigDecimal.ZERO;
        }

        PaymentMethod paymentMethod = null;
        if (paymentMethodStr != null && !paymentMethodStr.isBlank()) {
            try {
                paymentMethod = PaymentMethod.valueOf(paymentMethodStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Metode pembayaran tidak valid. Nilai yang diterima: CASH, TRANSFER");
            }
        }

        List<Map<String, String>> breakdownItems = parseAndValidateBreakdownList(breakdownListJson, isSubmit, amount);

        String documentUrl = pr.getSupportingDocumentUrl();
        String documentName = pr.getSupportingDocumentName();
        if (supportingDocument != null && !supportingDocument.isEmpty()) {
            String contentType = supportingDocument.getContentType();
            if (contentType == null ||
                    (!ALLOWED_IMAGE_TYPES.contains(contentType) && !ALLOWED_DOC_TYPES.contains(contentType))) {
                throw new IllegalArgumentException("Format file tidak didukung. Gunakan JPG, PNG, GIF, WebP, atau PDF (maks. 5MB)");
            }
            if (supportingDocument.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("Ukuran file melebihi batas 5MB");
            }

            try {
                String originalFilename = supportingDocument.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf("."))
                        : "";
                String storagePath = UUID.randomUUID() + extension;
                String resourceType = ALLOWED_DOC_TYPES.contains(contentType) ? "raw" : "image";
                documentUrl = cloudinaryStorageService.uploadFile(
                        supportingDocument, storagePath, CLOUDINARY_FOLDER, resourceType
                );
                documentName = originalFilename;
            } catch (IOException e) {
                throw new RuntimeException("Gagal mengupload dokumen pendukung: " + e.getMessage());
            }
        }

        pr.setTitle(title != null ? title.trim() : "");
        pr.setAmount(amount);
        pr.setExpenseCategory(expenseCategory);
        pr.setSubCategory(subCategory != null && !subCategory.isBlank() ? subCategory.trim() : null);
        pr.setNeededDate(neededDate);
        pr.setPaymentMethod(paymentMethod);
        pr.setNotes(notes != null && !notes.isBlank() ? notes.trim() : null);
        pr.setSupportingDocumentUrl(documentUrl);
        pr.setSupportingDocumentName(documentName);
        pr.setUpdatedBy(actor);

        pr.getBreakdowns().clear();
        List<PaymentRequestBreakdown> newBreakdowns = new ArrayList<>();
        for (Map<String, String> item : breakdownItems) {
            PaymentRequestBreakdown breakdown = PaymentRequestBreakdown.builder()
                    .paymentRequest(pr)
                    .description(item.get("description").trim())
                    .amount(new BigDecimal(item.get("amount").trim().replace(",", ".")))
                    .build();
            newBreakdowns.add(breakdown);
        }
        pr.getBreakdowns().addAll(newBreakdowns);

        if (isSubmit) {
            pr.setStatus(PaymentRequestStatus.PENDING_REVIEW);
            paymentRequestReviewActivityRepository.save(PaymentRequestReviewActivity.builder()
                    .paymentRequest(pr)
                    .actor(actor)
                    .status(PaymentRequestStatus.PENDING_REVIEW)
                    .note(null)
                    .build());
        }

        paymentRequestRepository.save(pr);

        return toResponseDTO(pr);
    }

    @Override
    @Transactional
    public void cancel(String id, String currentUsername) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Format ID tidak valid");
        }

        PaymentRequest pr = paymentRequestRepository.findById(uuid)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Ticket tidak ditemukan"));

        User actor = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        if (pr.getCreatedBy() == null
                || pr.getCreatedBy().getUsername() == null
                || !pr.getCreatedBy().getUsername().equalsIgnoreCase(actor.getUsername())) {
            throw new PaymentRequestAccessForbiddenException("Tidak memiliki akses");
        }

        if (pr.getStatus() != PaymentRequestStatus.DRAFT && pr.getStatus() != PaymentRequestStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Ticket tidak dapat dibatalkan karena sudah diproses");
        }

        pr.setStatus(PaymentRequestStatus.CANCELLED);
        pr.setCancelledAt(LocalDateTime.now());
        pr.setCancelledBy(actor);
        pr.setUpdatedBy(actor);
        paymentRequestRepository.save(pr);

        paymentRequestReviewActivityRepository.save(PaymentRequestReviewActivity.builder()
                .paymentRequest(pr)
                .actor(actor)
                .status(PaymentRequestStatus.CANCELLED)
                .note(null)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentRequestDetailResponseDTO getById(String id, String currentUsername) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Format ID tidak valid");
        }

        PaymentRequest paymentRequest = paymentRequestRepository.findDetailById(uuid)
                .orElse(null);

        if (paymentRequest == null) {
            return null;
        }

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        assertCanViewDetail(paymentRequest, currentUser);

        List<PaymentRequestReviewActivity> activities =
                paymentRequestReviewActivityRepository.findByPaymentRequest_IdOrderByCreatedAtAsc(uuid);

        return toDetailResponseDTO(paymentRequest, activities);
    }

    @Override
    @Transactional
    public PaymentRequestDetailResponseDTO approve(String id, String reviewNote, String currentUsername) {
        return processReviewAction(id, reviewNote, currentUsername, PaymentRequestStatus.APPROVED, false);
    }

    @Override
    @Transactional
    public PaymentRequestDetailResponseDTO reject(String id, String reviewNote, String currentUsername) {
        return processReviewAction(id, reviewNote, currentUsername, PaymentRequestStatus.REJECTED, true);
    }

    @Override
    @Transactional
    public PaymentRequestDetailResponseDTO requestRevision(String id, String reviewNote, String currentUsername) {
        return processReviewAction(id, reviewNote, currentUsername, PaymentRequestStatus.REVISION_REQUESTED, true);
    }

    private PaymentRequestDetailResponseDTO processReviewAction(
            String id,
            String reviewNote,
            String currentUsername,
            PaymentRequestStatus targetStatus,
            boolean reviewNoteRequired
    ) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Format ID tidak valid");
        }

        PaymentRequest pr = paymentRequestRepository.findDetailById(uuid)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Ticket tidak ditemukan"));

        User reviewer = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        String reviewerRole = reviewer.getRole() == null ? "" : reviewer.getRole().trim();
        if (!ROLE_KETUA_YAYASAN.equalsIgnoreCase(reviewerRole)) {
            throw new PaymentRequestAccessForbiddenException("Tidak memiliki akses");
        }

        if (pr.getStatus() != PaymentRequestStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Ticket sudah diproses / status tidak valid");
        }

        String normalizedReviewNote = reviewNote == null ? null : reviewNote.trim();
        if (reviewNoteRequired && (normalizedReviewNote == null || normalizedReviewNote.isBlank())) {
            throw new IllegalArgumentException("Catatan review wajib diisi");
        }
        if (normalizedReviewNote != null && normalizedReviewNote.isBlank()) {
            normalizedReviewNote = null;
        }

        pr.setStatus(targetStatus);
        pr.setUpdatedBy(reviewer);
        paymentRequestRepository.save(pr);

        paymentRequestReviewActivityRepository.save(PaymentRequestReviewActivity.builder()
                .paymentRequest(pr)
                .actor(reviewer)
                .status(targetStatus)
                .note(normalizedReviewNote)
                .build());

        List<PaymentRequestReviewActivity> activities =
                paymentRequestReviewActivityRepository.findByPaymentRequest_IdOrderByCreatedAtAsc(uuid);

        return toDetailResponseDTO(pr, activities);
    }

    private void assertCanViewDetail(PaymentRequest paymentRequest, User viewer) {
        String role = viewer.getRole() == null ? "" : viewer.getRole().trim();
        boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(role);
        boolean isKetuaYayasan = ROLE_KETUA_YAYASAN.equalsIgnoreCase(role);
        boolean isPengurus = ROLE_PENGURUS.equalsIgnoreCase(role);

        if (isAdmin) {
            return;
        }
        if (isKetuaYayasan) {
            if (paymentRequest.getStatus() == PaymentRequestStatus.DRAFT) {
                throw new PaymentRequestAccessForbiddenException("Tidak memiliki akses");
            }
            return;
        }
        if (isPengurus) {
            if (paymentRequest.getCreatedBy() == null
                    || paymentRequest.getCreatedBy().getUsername() == null
                    || !paymentRequest.getCreatedBy().getUsername().equalsIgnoreCase(viewer.getUsername())) {
                throw new PaymentRequestAccessForbiddenException("Tidak memiliki akses");
            }
            return;
        }

        throw new PaymentRequestAccessForbiddenException("Tidak memiliki akses");
    }

    private PaymentRequestDetailResponseDTO toDetailResponseDTO(
            PaymentRequest pr,
            List<PaymentRequestReviewActivity> activities
    ) {
        List<PaymentRequestDetailResponseDTO.BreakdownItemDTO> breakdownDTOs = List.of();
        if (pr.getBreakdowns() != null && !pr.getBreakdowns().isEmpty()) {
            breakdownDTOs = pr.getBreakdowns().stream()
                    .map(b -> PaymentRequestDetailResponseDTO.BreakdownItemDTO.builder()
                            .id(b.getId())
                            .description(b.getDescription())
                            .amount(b.getAmount())
                            .build())
                    .toList();
        }

        List<PaymentRequestDetailResponseDTO.AttachmentDTO> attachments = new ArrayList<>();
        if (pr.getSupportingDocumentUrl() != null && !pr.getSupportingDocumentUrl().isBlank()) {
            attachments.add(PaymentRequestDetailResponseDTO.AttachmentDTO.builder()
                    .url(pr.getSupportingDocumentUrl())
                    .fileName(pr.getSupportingDocumentName())
                    .build());
        }

        User createdBy = pr.getCreatedBy();
        PaymentRequestDetailResponseDTO.CreatedByDTO createdByDto =
                PaymentRequestDetailResponseDTO.CreatedByDTO.builder()
                        .username(createdBy != null ? createdBy.getUsername() : null)
                        .name(createdBy != null ? createdBy.getNama() : null)
                        .role(createdBy != null ? createdBy.getRole() : null)
                        .build();

        List<PaymentRequestDetailResponseDTO.ReviewHistoryItemDTO> reviewHistory = activities.stream()
                .map(a -> PaymentRequestDetailResponseDTO.ReviewHistoryItemDTO.builder()
                        .status(a.getStatus().name())
                        .actorName(a.getActor() != null ? a.getActor().getNama() : null)
                        .actorRole(a.getActor() != null ? a.getActor().getRole() : null)
                        .actorUsername(a.getActor() != null ? a.getActor().getUsername() : null)
                        .note(a.getNote())
                        .occurredAt(a.getCreatedAt())
                        .build())
                .toList();

        return PaymentRequestDetailResponseDTO.builder()
                .id(pr.getId())
                .title(pr.getTitle())
                .expenseCategory(pr.getExpenseCategory().name())
                .expenseSubCategory(pr.getSubCategory())
                .amount(pr.getAmount())
                .breakdownList(breakdownDTOs)
                .neededDate(pr.getNeededDate())
                .paymentMethod(pr.getPaymentMethod() != null ? pr.getPaymentMethod().name() : null)
                .notes(pr.getNotes())
                .attachments(attachments)
                .status(pr.getStatus().name())
                .createdAt(pr.getCreatedAt())
                .updatedAt(pr.getUpdatedAt())
                .updatedByUsername(pr.getUpdatedBy() != null ? pr.getUpdatedBy().getUsername() : null)
                .createdBy(createdByDto)
                .reviewHistory(reviewHistory)
                .build();
    }

    private void validateSubCategoryIfPresent(ExpenseCategory expenseCategory, String subCategory) {
        if (subCategory == null || subCategory.isBlank()) {
            return;
        }
        if (!ExpenseSubcategories.isAllowed(expenseCategory, subCategory)) {
            throw new IllegalArgumentException(
                    "Sub-kategori tidak valid untuk kategori yang dipilih. Nilai yang diperbolehkan: "
                            + ExpenseSubcategories.allowedListHint(expenseCategory)
            );
        }
    }

    /**
     * Parses breakdown JSON as a list of objects (so numeric amounts from clients are handled),
     * rejects partial rows and non-positive amounts, and enforces total vs header amount.
     */
    private List<Map<String, String>> parseAndValidateBreakdownList(
            String breakdownListJson,
            boolean isSubmit,
            BigDecimal headerAmount
    ) {
        List<Map<String, Object>> rawRows = new ArrayList<>();
        if (breakdownListJson != null && !breakdownListJson.isBlank()) {
            try {
                rawRows = objectMapper.readValue(breakdownListJson, new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                throw new IllegalArgumentException("Format rincian penggunaan dana tidak valid");
            }
        }

        List<Map<String, String>> normalized = new ArrayList<>();
        int rowIndex = 0;
        for (Map<String, Object> row : rawRows) {
            rowIndex++;
            String desc = stringifyBreakdownField(row.get("description"));
            String amountRaw = stringifyBreakdownAmount(row.get("amount"));
            boolean descEmpty = desc.isBlank();
            boolean amtEmpty = amountRaw.isBlank();

            if (descEmpty && amtEmpty) {
                continue;
            }
            if (descEmpty || amtEmpty) {
                throw new IllegalArgumentException(
                        "Rincian penggunaan dana item ke-" + rowIndex
                                + ": deskripsi dan nominal wajib diisi lengkap untuk setiap baris yang digunakan"
                );
            }

            BigDecimal itemAmount;
            try {
                itemAmount = new BigDecimal(amountRaw.trim().replace(",", "."));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Nominal rincian item ke-" + rowIndex + " harus berupa angka");
            }
            if (itemAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Nominal rincian item ke-" + rowIndex + " harus lebih dari 0");
            }

            Map<String, String> saved = new LinkedHashMap<>();
            saved.put("description", desc.trim());
            saved.put("amount", itemAmount.stripTrailingZeros().toPlainString());
            normalized.add(saved);
        }

        if (isSubmit) {
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("Rincian penggunaan dana wajib diisi minimal satu item");
            }
            if (headerAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Nominal penggunaan dana harus lebih dari 0");
            }
        }

        BigDecimal breakdownTotal = BigDecimal.ZERO;
        for (Map<String, String> item : normalized) {
            breakdownTotal = breakdownTotal.add(new BigDecimal(item.get("amount")));
        }

        if (isSubmit) {
            if (breakdownTotal.compareTo(headerAmount) != 0) {
                throw new IllegalArgumentException(
                        "Total rincian (Rp " + breakdownTotal.toPlainString() + ") harus sama dengan nominal penggunaan dana (Rp "
                                + headerAmount.toPlainString() + ")"
                );
            }
        } else {
            if (!normalized.isEmpty() && headerAmount.compareTo(BigDecimal.ZERO) > 0
                    && breakdownTotal.compareTo(headerAmount) != 0) {
                throw new IllegalArgumentException(
                        "Total rincian (Rp " + breakdownTotal.toPlainString() + ") harus sama dengan nominal penggunaan dana (Rp "
                                + headerAmount.toPlainString() + ")"
                );
            }
        }

        return normalized;
    }

    private static String stringifyBreakdownField(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private static String stringifyBreakdownAmount(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal bd) {
            return bd.stripTrailingZeros().toPlainString();
        }
        if (value instanceof Number n) {
            return new BigDecimal(n.toString()).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value).trim();
    }

    private PaymentRequestResponseDTO toResponseDTO(PaymentRequest pr) {
        List<PaymentRequestResponseDTO.BreakdownItemDTO> breakdownDTOs = null;
        if (pr.getBreakdowns() != null && !pr.getBreakdowns().isEmpty()) {
            breakdownDTOs = pr.getBreakdowns().stream()
                    .map(b -> PaymentRequestResponseDTO.BreakdownItemDTO.builder()
                            .id(b.getId())
                            .description(b.getDescription())
                            .amount(b.getAmount())
                            .build())
                    .toList();
        }

        return PaymentRequestResponseDTO.builder()
                .id(pr.getId())
                .title(pr.getTitle())
                .amount(pr.getAmount())
                .expenseCategory(pr.getExpenseCategory().name())
                .subCategory(pr.getSubCategory())
                .neededDate(pr.getNeededDate())
                .paymentMethod(pr.getPaymentMethod() != null ? pr.getPaymentMethod().name() : null)
                .notes(pr.getNotes())
                .supportingDocumentUrl(pr.getSupportingDocumentUrl())
                .supportingDocumentName(pr.getSupportingDocumentName())
                .breakdowns(breakdownDTOs)
                .status(pr.getStatus().name())
                .createdByUsername(pr.getCreatedBy().getUsername())
                .createdAt(pr.getCreatedAt())
                .updatedAt(pr.getUpdatedAt())
                .updatedByUsername(pr.getUpdatedBy() != null ? pr.getUpdatedBy().getUsername() : null)
                .build();
    }
}
