package io.propenuy.asis_app_be.restservice;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import io.propenuy.asis_app_be.model.User;
import io.propenuy.asis_app_be.model.enums.ExpenseCategory;
import io.propenuy.asis_app_be.model.enums.PaymentMethod;
import io.propenuy.asis_app_be.model.enums.PaymentRequestStatus;
import io.propenuy.asis_app_be.repository.PaymentRequestRepository;
import io.propenuy.asis_app_be.repository.UserRepository;
import io.propenuy.asis_app_be.restdto.response.PaymentRequestListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.PaymentRequestResponseDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentRequestRestServiceImpl implements PaymentRequestRestService {

    private final PaymentRequestRepository paymentRequestRepository;
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

        // --- Validation ---
        if (isSubmit) {
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("Judul pengajuan wajib diisi");
            }
            if (neededDateStr == null || neededDateStr.isBlank()) {
                throw new IllegalArgumentException("Tanggal pengajuan wajib diisi");
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
                throw new IllegalArgumentException("Minimal isi judul, tanggal pengajuan, dan kategori untuk menyimpan draft.");
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
                        "Kategori tidak valid. Nilai yang diterima: OPERASIONAL, KONSUMSI, TRANSPORTASI, PERLENGKAPAN, PROGRAM_KEGIATAN, GAJI, INFRASTRUKTUR, LAIN_LAIN"
                );
            }
        } else {
            expenseCategory = ExpenseCategory.LAIN_LAIN;
        }

        // Parse neededDate (tanggal pengajuan)
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

        // Parse breakdown list
        List<Map<String, String>> breakdownItems = new ArrayList<>();
        if (breakdownListJson != null && !breakdownListJson.isBlank()) {
            try {
                breakdownItems = objectMapper.readValue(breakdownListJson, new TypeReference<>() {});
            } catch (Exception e) {
                throw new IllegalArgumentException("Format rincian penggunaan dana tidak valid");
            }
        }

        if (isSubmit) {
            if (breakdownItems.isEmpty()) {
                throw new IllegalArgumentException("Rincian penggunaan dana wajib diisi minimal satu item");
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Nominal penggunaan dana harus lebih dari 0");
            }
        }

        // Validate breakdown items (strict on submit; draft keeps only complete rows)
        BigDecimal breakdownTotal = BigDecimal.ZERO;
        if (isSubmit) {
            for (int i = 0; i < breakdownItems.size(); i++) {
                Map<String, String> item = breakdownItems.get(i);
                String desc = item.get("description");
                String itemAmountStr = item.get("amount");

                if (desc == null || desc.isBlank()) {
                    throw new IllegalArgumentException("Deskripsi rincian item ke-" + (i + 1) + " wajib diisi");
                }
                if (itemAmountStr == null || itemAmountStr.isBlank()) {
                    throw new IllegalArgumentException("Nominal rincian item ke-" + (i + 1) + " wajib diisi");
                }

                BigDecimal itemAmount;
                try {
                    itemAmount = new BigDecimal(itemAmountStr.trim().replace(",", "."));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Nominal rincian item ke-" + (i + 1) + " harus berupa angka");
                }
                if (itemAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Nominal rincian item ke-" + (i + 1) + " harus lebih dari 0");
                }

                breakdownTotal = breakdownTotal.add(itemAmount);
            }
            if (breakdownTotal.compareTo(amount) != 0) {
                throw new IllegalArgumentException(
                        "Total rincian (Rp " + breakdownTotal.toPlainString() + ") harus sama dengan nominal penggunaan dana (Rp " + amount.toPlainString() + ")"
                );
            }
        } else {
            List<Map<String, String>> completeOnly = new ArrayList<>();
            for (Map<String, String> item : breakdownItems) {
                String desc = item.get("description");
                String itemAmountStr = item.get("amount");
                if (desc != null && !desc.isBlank() && itemAmountStr != null && !itemAmountStr.isBlank()) {
                    completeOnly.add(item);
                }
            }
            breakdownItems = completeOnly;
            for (Map<String, String> item : breakdownItems) {
                try {
                    BigDecimal itemAmount = new BigDecimal(item.get("amount").trim().replace(",", "."));
                    breakdownTotal = breakdownTotal.add(itemAmount);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Format rincian penggunaan dana tidak valid");
                }
            }
            if (!breakdownItems.isEmpty() && amount.compareTo(BigDecimal.ZERO) > 0
                    && breakdownTotal.compareTo(amount) != 0) {
                throw new IllegalArgumentException(
                        "Total rincian (Rp " + breakdownTotal.toPlainString() + ") harus sama dengan nominal penggunaan dana (Rp " + amount.toPlainString() + ")"
                );
            }
        }

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
                .purpose("")
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

        return toResponseDTO(paymentRequest);
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
                .build();
    }
}
