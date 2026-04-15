package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.model.IncomeTransaction;
import io.propenuy.asis_app_be.model.User;
import io.propenuy.asis_app_be.model.enums.IncomeCategory;
import io.propenuy.asis_app_be.model.enums.PaymentMethod;
import io.propenuy.asis_app_be.model.enums.SourceType;
import io.propenuy.asis_app_be.repository.IncomeTransactionRepository;
import io.propenuy.asis_app_be.repository.UserRepository;
import io.propenuy.asis_app_be.restdto.response.IncomeTransactionListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.IncomeTransactionResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IncomeTransactionRestServiceImpl implements IncomeTransactionRestService {

    private final IncomeTransactionRepository incomeTransactionRepository;
    private final UserRepository userRepository;
    private final CloudinaryStorageService cloudinaryStorageService;

    private static final String CLOUDINARY_FOLDER = "income-proofs";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String ROLE_PENGURUS = "PENGURUS";
    private static final String ROLE_KETUA_YAYASAN = "KETUA YAYASAN";
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final List<String> ALLOWED_DOC_TYPES = List.of(
            "application/pdf"
    );

    @Override
    @Transactional
    public IncomeTransactionResponseDTO create(
            String transactionDateStr,
            String category,
            String sourceType,
            String paymentMethod,
            String amountStr,
            String note,
            String donorName,
            MultipartFile proofFile,
            String currentUsername
    ) {
        // Validasi required fields
        if (transactionDateStr == null || transactionDateStr.isBlank()) {
            throw new IllegalArgumentException("Tanggal transaksi wajib diisi");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Kategori pemasukan wajib diisi");
        }
        if (sourceType == null || sourceType.isBlank()) {
            throw new IllegalArgumentException("Sumber donasi wajib diisi");
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("Metode pembayaran wajib diisi");
        }
        if (amountStr == null || amountStr.isBlank()) {
            throw new IllegalArgumentException("Nominal wajib diisi");
        }
        if (proofFile == null || proofFile.isEmpty()) {
            throw new IllegalArgumentException("Upload bukti transaksi wajib");
        }

        // Parse & validate category
        IncomeCategory incomeCategory;
        try {
            String catUpper = category.toUpperCase().replace("-", "_");
            incomeCategory = IncomeCategory.valueOf(catUpper);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Kategori tidak valid. Nilai yang diterima: DONASI, ZAKAT, INFAQ, LAIN_LAIN");
        }

        // Parse & validate payment method
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(paymentMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Metode pembayaran tidak valid. Nilai yang diterima: CASH, TRANSFER");
        }

        // Parse & validate source type
        SourceType srcType;
        try {
            srcType = SourceType.valueOf(sourceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Sumber donasi tidak valid. Nilai yang diterima: INDIVIDU, KOMUNITAS, PERUSAHAAN");
        }

        // Parse date
        LocalDate transactionDate;
        try {
            transactionDate = LocalDate.parse(transactionDateStr.trim(), DATE_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Format tanggal tidak valid. Gunakan format YYYY-MM-DD");
        }

        // Parse & validate amount
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nominal harus berupa angka");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Nominal harus lebih dari 0");
        }

        // Validate file type
        String contentType = proofFile.getContentType();
        if (contentType == null || (!ALLOWED_IMAGE_TYPES.contains(contentType) && !ALLOWED_DOC_TYPES.contains(contentType))) {
            throw new IllegalArgumentException("Format file tidak didukung. Gunakan gambar (JPEG, PNG, GIF, WebP) atau PDF");
        }

        // Get current user
        User createdByUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        // Upload file ke Cloudinary
        // PDF -> resource_type "raw" agar browser bisa render langsung
        // Image -> resource_type "image"
        String proofFilePath;
        try {
            String originalFilename = proofFile.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String storagePath = UUID.randomUUID() + extension;
            String resourceType = ALLOWED_DOC_TYPES.contains(contentType) ? "raw" : "image";
            proofFilePath = cloudinaryStorageService.uploadFile(
                    proofFile, storagePath, CLOUDINARY_FOLDER, resourceType
            );
        } catch (IOException e) {
            throw new RuntimeException("Gagal mengupload file bukti ke Cloudinary: " + e.getMessage());
        }

        IncomeTransaction transaction = IncomeTransaction.builder()
                .transactionDate(transactionDate)
                .category(incomeCategory)
                .sourceType(srcType)
                .paymentMethod(method)
                .amount(amount)
                .donorName(donorName != null && !donorName.isBlank() ? donorName.trim() : null)
                .note(note != null && !note.isBlank() ? note.trim() : null)
                .proofFilePath(proofFilePath)
                .status("CONFIRMED")
                .createdBy(createdByUser)
                .build();

        incomeTransactionRepository.save(transaction);

        return toResponseDTO(transaction);
    }

    @Override
    @Transactional
    public void softDelete(UUID id, String currentUsername) {
        IncomeTransaction transaction = incomeTransactionRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Transaksi tidak ditemukan"));
        if ("INACTIVE".equals(transaction.getStatus())) {
            throw new IllegalArgumentException("Transaksi sudah dinonaktifkan");
        }
        User deletedByUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        if (!hasRole(deletedByUser, ROLE_KETUA_YAYASAN)) {
            throw new IllegalArgumentException("Hanya Ketua Yayasan yang dapat menonaktifkan transaksi pemasukan");
        }
        transaction.setStatus("INACTIVE");
        transaction.setDeletedAt(LocalDateTime.now());
        transaction.setDeletedBy(deletedByUser);
        incomeTransactionRepository.save(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public IncomeTransactionListResponseDTO list(
            String startDateStr,
            String endDateStr,
            String category,
            String paymentMethod,
            String sourceType,
            String search,
            int page,
            int size
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

        final IncomeCategory incomeCategory;
        if (category != null && !category.isBlank()) {
            try {
                String catUpper = category.toUpperCase().replace("-", "_");
                incomeCategory = IncomeCategory.valueOf(catUpper);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Kategori tidak valid. Nilai yang diterima: DONASI, ZAKAT, INFAQ, LAIN_LAIN");
            }
        } else {
            incomeCategory = null;
        }

        final PaymentMethod method;
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            try {
                method = PaymentMethod.valueOf(paymentMethod.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Metode pembayaran tidak valid. Nilai yang diterima: CASH, TRANSFER");
            }
        } else {
            method = null;
        }

        final SourceType srcType;
        if (sourceType != null && !sourceType.isBlank()) {
            try {
                srcType = SourceType.valueOf(sourceType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Sumber donasi tidak valid. Nilai yang diterima: INDIVIDU, KOMUNITAS, PERUSAHAAN");
            }
        } else {
            srcType = null;
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size > 0 ? size : 10,
                Sort.by(Sort.Direction.DESC, "transactionDate", "createdAt")
        );

        Specification<IncomeTransaction> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), "CONFIRMED"));

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), endDate));
            }
            if (incomeCategory != null) {
                predicates.add(cb.equal(root.get("category"), incomeCategory));
            }
            if (method != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), method));
            }
            if (srcType != null) {
                predicates.add(cb.equal(root.get("sourceType"), srcType));
            }

            // Pencarian teks: donorName atau createdBy.username
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("donorName")), pattern),
                        cb.like(cb.lower(root.join("createdBy").get("username")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<IncomeTransaction> pageResult = incomeTransactionRepository.findAll(spec, pageable);

        List<IncomeTransactionResponseDTO> content = pageResult.getContent().stream()
                .map(this::toResponseDTO)
                .toList();

        return IncomeTransactionListResponseDTO.builder()
                .content(content)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
    }



    @Override
    @Transactional(readOnly = true)
    public IncomeTransactionResponseDTO getById(UUID id) {
        IncomeTransaction transaction = incomeTransactionRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Transaksi tidak ditemukan"));
        if (!"CONFIRMED".equals(transaction.getStatus())) {
            throw new jakarta.persistence.EntityNotFoundException("Transaksi tidak ditemukan");
        }
        return toResponseDTO(transaction);
    }

    @Override
    @Transactional
    public IncomeTransactionResponseDTO update(
            UUID id,
            String transactionDateStr,
            String category,
            String sourceType,
            String paymentMethod,
            String amountStr,
            String note,
            String donorName,
            MultipartFile proofFile,
            String currentUsername
    ) {
        IncomeTransaction transaction = incomeTransactionRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Transaksi tidak ditemukan"));

        if (!"CONFIRMED".equals(transaction.getStatus())) {
            throw new IllegalArgumentException("Transaksi sudah dinonaktifkan dan tidak dapat diubah");
        }

        User updatedByUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        boolean isKetua = hasRole(updatedByUser, ROLE_KETUA_YAYASAN);
        boolean isPengurus = hasRole(updatedByUser, ROLE_PENGURUS);

        if (!isKetua && !isPengurus) {
            throw new IllegalArgumentException("Anda tidak memiliki akses untuk mengubah transaksi ini");
        }

        if (isPengurus) {
            if (transaction.getCreatedBy() == null
                    || transaction.getCreatedBy().getUserId() == null
                    || !transaction.getCreatedBy().getUserId().equals(updatedByUser.getUserId())) {
                throw new IllegalArgumentException("Pengurus hanya dapat mengubah transaksi yang dibuat sendiri");
            }

            LocalDateTime createdAt = transaction.getCreatedAt();
            if (createdAt == null || Duration.between(createdAt, LocalDateTime.now()).toMinutes() > 30) {
                throw new IllegalArgumentException("Batas waktu edit 30 menit telah terlewati");
            }

            int editCount = transaction.getPengurusEditCount() == null ? 0 : transaction.getPengurusEditCount();
            if (editCount >= 1) {
                throw new IllegalArgumentException("Kuota edit Pengurus untuk transaksi ini sudah habis");
            }
        }

        if (transactionDateStr == null || transactionDateStr.isBlank()) {
            throw new IllegalArgumentException("Tanggal transaksi wajib diisi");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Kategori pemasukan wajib diisi");
        }
        if (sourceType == null || sourceType.isBlank()) {
            throw new IllegalArgumentException("Sumber donasi wajib diisi");
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("Metode pembayaran wajib diisi");
        }
        if (amountStr == null || amountStr.isBlank()) {
            throw new IllegalArgumentException("Nominal wajib diisi");
        }

        IncomeCategory incomeCategory;
        try {
            String catUpper = category.toUpperCase().replace("-", "_");
            incomeCategory = IncomeCategory.valueOf(catUpper);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Kategori tidak valid. Nilai yang diterima: DONASI, ZAKAT, INFAQ, LAIN_LAIN");
        }

        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(paymentMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Metode pembayaran tidak valid. Nilai yang diterima: CASH, TRANSFER");
        }

        SourceType srcType;
        try {
            srcType = SourceType.valueOf(sourceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Sumber donasi tidak valid. Nilai yang diterima: INDIVIDU, KOMUNITAS, PERUSAHAAN");
        }

        LocalDate transactionDate;
        try {
            transactionDate = LocalDate.parse(transactionDateStr.trim(), DATE_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Format tanggal tidak valid. Gunakan format YYYY-MM-DD");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nominal harus berupa angka");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Nominal harus lebih dari 0");
        }

        String proofFilePath = transaction.getProofFilePath();
        if (proofFile != null && !proofFile.isEmpty()) {
            String contentType = proofFile.getContentType();
            if (contentType == null || (!ALLOWED_IMAGE_TYPES.contains(contentType) && !ALLOWED_DOC_TYPES.contains(contentType))) {
                throw new IllegalArgumentException("Format file tidak didukung. Gunakan gambar (JPEG, PNG, GIF, WebP) atau PDF");
            }
            try {
                String originalFilename = proofFile.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf("."))
                        : "";
                String storagePath = UUID.randomUUID() + extension;
                String resourceType = ALLOWED_DOC_TYPES.contains(contentType) ? "raw" : "image";
                proofFilePath = cloudinaryStorageService.uploadFile(
                        proofFile, storagePath, CLOUDINARY_FOLDER, resourceType
                );
            } catch (IOException e) {
                throw new RuntimeException("Gagal mengupload file bukti ke Cloudinary: " + e.getMessage());
            }
        }

        transaction.setTransactionDate(transactionDate);
        transaction.setCategory(incomeCategory);
        transaction.setSourceType(srcType);
        transaction.setPaymentMethod(method);
        transaction.setAmount(amount);
        transaction.setDonorName(donorName != null && !donorName.isBlank() ? donorName.trim() : null);
        transaction.setNote(note != null && !note.isBlank() ? note.trim() : null);
        transaction.setProofFilePath(proofFilePath);
        transaction.setUpdatedBy(updatedByUser);
        if (isPengurus) {
            int editCount = transaction.getPengurusEditCount() == null ? 0 : transaction.getPengurusEditCount();
            transaction.setPengurusEditCount(editCount + 1);
        }

        incomeTransactionRepository.save(transaction);

        return toResponseDTO(transaction);
    }

    private boolean hasRole(User user, String role) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return role.equalsIgnoreCase(user.getRole().trim());
    }

    private IncomeTransactionResponseDTO toResponseDTO(IncomeTransaction t) {
        return IncomeTransactionResponseDTO.builder()
                .id(t.getId())
                .transactionDate(t.getTransactionDate())
                .category(t.getCategory().name())
                .sourceType(t.getSourceType().name())
                .paymentMethod(t.getPaymentMethod().name())
                .amount(t.getAmount())
                .donorName(t.getDonorName())
                .note(t.getNote())
                .proofFilePath(t.getProofFilePath())
                .status(t.getStatus())
                .createdByUsername(t.getCreatedBy().getUsername())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .updatedByUsername(t.getUpdatedBy() != null ? t.getUpdatedBy().getUsername() : null)
                .build();
    }
}
