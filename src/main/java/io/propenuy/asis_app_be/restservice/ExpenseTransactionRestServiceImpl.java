package io.propenuy.asis_app_be.restservice;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import io.propenuy.asis_app_be.model.ExpenseTransaction;
import io.propenuy.asis_app_be.model.User;
import io.propenuy.asis_app_be.model.enums.ExpenseCategory;
import io.propenuy.asis_app_be.model.enums.PaymentMethod;
import io.propenuy.asis_app_be.repository.ExpenseTransactionRepository;
import io.propenuy.asis_app_be.repository.IncomeTransactionRepository;
import io.propenuy.asis_app_be.repository.UserRepository;
import io.propenuy.asis_app_be.restdto.response.ExpenseTransactionListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.ExpenseTransactionResponseDTO;
import io.propenuy.asis_app_be.restservice.ExpenseCategoryRules;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseTransactionRestServiceImpl implements ExpenseTransactionRestService {

    private final ExpenseTransactionRepository expenseTransactionRepository;
    private final IncomeTransactionRepository incomeTransactionRepository;
    private final UserRepository userRepository;
    private final CloudinaryStorageService cloudinaryStorageService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final List<String> ALLOWED_DOC_TYPES = List.of(
            "application/pdf"
    );

    private static final String CLOUDINARY_FOLDER = "expense-proofs";
    private static final String ROLE_PENGURUS = "PENGURUS";
    private static final String ROLE_KETUA_YAYASAN = "KETUA YAYASAN";

    @Override
    @Transactional
    public ExpenseTransactionResponseDTO create(
            String transactionDateStr,
            String category,
            String subCategoryStr,
            String paymentMethod,
            String amountStr,
            String note,
            MultipartFile proofFile,
            String currentUsername
    ) {
        if (transactionDateStr == null || transactionDateStr.isBlank()) {
            throw new IllegalArgumentException("Tanggal transaksi wajib diisi");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Kategori pengeluaran wajib diisi");
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

        User createdByUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        if (hasRole(createdByUser, ROLE_KETUA_YAYASAN)) {
            throw new ExpenseEditForbiddenException("Ketua Yayasan tidak dapat membuat transaksi pengeluaran baru");
        }

        ExpenseCategory expenseCategory;
        try {
            String catUpper = category.toUpperCase().replace("-", "_").replace(" ", "_");
            expenseCategory = ExpenseCategory.valueOf(catUpper);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Kategori tidak valid. Nilai yang dikenali: "
                            + Arrays.stream(ExpenseCategory.values())
                                    .map(Enum::name)
                                    .collect(Collectors.joining(", "))
            );
        }

        String normalizedSub = ExpenseCategoryRules.normalizeSubCategory(subCategoryStr);
        ExpenseCategoryRules.validateSubForMain(expenseCategory, normalizedSub);

        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(paymentMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Metode pembayaran tidak valid. Nilai yang diterima: CASH, TRANSFER");
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

        String contentType = proofFile.getContentType();
        if (contentType == null ||
                (!ALLOWED_IMAGE_TYPES.contains(contentType) && !ALLOWED_DOC_TYPES.contains(contentType))) {
            throw new IllegalArgumentException("Format file tidak didukung. Gunakan gambar (JPEG, PNG, GIF, WebP) atau PDF");
        }
        BigDecimal totalIncome = incomeTransactionRepository.sumAllConfirmedIncome();
        BigDecimal totalExpense = expenseTransactionRepository.sumAllActiveExpenses();
        BigDecimal currentBalance = totalIncome.subtract(totalExpense);

        if (currentBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Saldo kas tidak mencukupi. Saldo saat ini: " + currentBalance + ", jumlah pengeluaran: " + amount
            );
        }

        String proofFilePath = saveProofFile(proofFile);
        ExpenseTransaction transaction = ExpenseTransaction.builder()
                .transactionDate(transactionDate)
                .category(expenseCategory)
                .subCategory(normalizedSub)
                .amount(amount)
                .paymentMethod(method)
                .note(note != null && !note.isBlank() ? note.trim() : null)
                .proofFilePath(proofFilePath)
                .status("ACTIVE")
                .createdBy(createdByUser)
                .build();

        expenseTransactionRepository.save(transaction);

        return toResponseDTO(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseTransactionListResponseDTO list(
            String startDateStr,
            String endDateStr,
            String category,
            String paymentMethod,
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
        
        final ExpenseCategory expenseCategoryFilter;
        if (category != null && !category.isBlank()) {
            try {
                String catUpper = category.toUpperCase().replace("-", "_").replace(" ", "_");
                expenseCategoryFilter = ExpenseCategory.valueOf(catUpper);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Kategori tidak valid. Nilai yang dikenali: "
                                + Arrays.stream(ExpenseCategory.values())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", "))
                );
            }
        } else {
            expenseCategoryFilter = null;
        }

        final String searchTerm = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                size > 0 ? size : 10,
                Sort.by(Sort.Direction.DESC, "transactionDate", "createdAt")
        );

        Specification<ExpenseTransaction> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), endDate));
            }
            if (expenseCategoryFilter != null) {
                predicates.add(cb.equal(root.get("category"), expenseCategoryFilter));
            }
            if (method != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), method));
            }
            if (searchTerm != null) {
                predicates.add(cb.like(
                        cb.lower(cb.coalesce(root.get("note"), cb.literal(""))),
                        "%" + searchTerm + "%"
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<ExpenseTransaction> pageResult = expenseTransactionRepository.findAll(spec, pageable);

        List<ExpenseTransactionResponseDTO> content = pageResult.getContent().stream()
                .map(this::toResponseDTO)
                .toList();

        return ExpenseTransactionListResponseDTO.builder()
                .content(content)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseTransactionResponseDTO getById(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Format ID tidak valid");
        }

        ExpenseTransaction transaction = expenseTransactionRepository.findById(uuid)
                .orElse(null);

        if (transaction == null || !"ACTIVE".equals(transaction.getStatus())) {
            return null;
        }

        return toResponseDTO(transaction);
    }

    @Override
    @Transactional
    public ExpenseTransactionResponseDTO update(
            String id,
            String category,
            String subCategoryStr,
            String note,
            String currentUsername
    ) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Format ID tidak valid");
        }

        ExpenseTransaction transaction = expenseTransactionRepository.findById(uuid)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Transaksi pengeluaran tidak ditemukan"));

        if ("INACTIVE".equalsIgnoreCase(transaction.getStatus())) {
            throw new IllegalStateException("Transaksi pengeluaran sudah nonaktif dan tidak dapat diubah");
        }

        if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Nominal transaksi tidak valid (harus lebih dari 0)");
        }
        if (transaction.getProofFilePath() == null || transaction.getProofFilePath().isBlank()) {
            throw new IllegalArgumentException("Bukti transaksi wajib tersedia");
        }

        User updatedByUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        boolean isKetua = hasRole(updatedByUser, ROLE_KETUA_YAYASAN);
        boolean isPengurus = hasRole(updatedByUser, ROLE_PENGURUS);
        if (!isKetua && !isPengurus) {
            throw new ExpenseEditForbiddenException("Anda tidak memiliki akses untuk mengubah transaksi ini");
        }

        if (isPengurus) {
            if (transaction.getCreatedBy() == null
                    || transaction.getCreatedBy().getUserId() == null
                    || !transaction.getCreatedBy().getUserId().equals(updatedByUser.getUserId())) {
                throw new ExpenseEditForbiddenException("Pengurus hanya dapat mengedit transaksi yang dibuat sendiri");
            }
            LocalDateTime createdAt = transaction.getCreatedAt();
            if (createdAt == null || Duration.between(createdAt, LocalDateTime.now()).toMinutes() > 30) {
                throw new ExpenseEditForbiddenException("Batas waktu edit telah habis");
            }
        }

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Kategori pengeluaran wajib diisi");
        }

        ExpenseCategory expenseCategory;
        try {
            String catUpper = category.toUpperCase().replace("-", "_").replace(" ", "_");
            expenseCategory = ExpenseCategory.valueOf(catUpper);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Kategori tidak valid. Nilai yang dikenali: "
                            + Arrays.stream(ExpenseCategory.values())
                            .map(Enum::name)
                            .collect(Collectors.joining(", "))
            );
        }

        String normalizedSub = ExpenseCategoryRules.normalizeSubCategory(subCategoryStr);
        ExpenseCategoryRules.validateSubForMain(expenseCategory, normalizedSub);

        transaction.setCategory(expenseCategory);
        transaction.setSubCategory(normalizedSub);
        transaction.setNote(note != null && !note.isBlank() ? note.trim() : null);
        transaction.setUpdatedBy(updatedByUser);

        expenseTransactionRepository.save(transaction);
        return toResponseDTO(transaction);
    }

    @Override
    @Transactional
    public void softDelete(String id, String currentUsername) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Format ID tidak valid");
        }

        ExpenseTransaction transaction = expenseTransactionRepository.findById(uuid)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Transaksi pengeluaran tidak ditemukan"));

        if ("INACTIVE".equalsIgnoreCase(transaction.getStatus())) {
            throw new IllegalStateException("Transaksi pengeluaran sudah nonaktif");
        }

        User deletedByUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        if (!hasRole(deletedByUser, ROLE_KETUA_YAYASAN)) {
            throw new ExpenseEditForbiddenException("Hanya Ketua Yayasan yang dapat menonaktifkan transaksi pengeluaran");
        }

        transaction.setStatus("INACTIVE");
        transaction.setDeletedAt(LocalDateTime.now());
        transaction.setDeletedBy(deletedByUser);
        transaction.setUpdatedBy(deletedByUser);
        expenseTransactionRepository.save(transaction);
    }

    // Upload bukti file ke Cloudinary
    private String saveProofFile(MultipartFile file) {
        try {
            String contentType = file.getContentType();
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String storagePath = UUID.randomUUID() + extension;
            String resourceType = ALLOWED_DOC_TYPES.contains(contentType) ? "raw" : "image";
            return cloudinaryStorageService.uploadFile(
                    file, storagePath, CLOUDINARY_FOLDER, resourceType
            );
        } catch (IOException e) {
            throw new RuntimeException("Gagal mengupload file bukti ke Cloudinary: " + e.getMessage());
        }
    }

    private ExpenseTransactionResponseDTO toResponseDTO(ExpenseTransaction t) {
        return ExpenseTransactionResponseDTO.builder()
                .id(t.getId())
                .transactionDate(t.getTransactionDate())
                .category(t.getCategory().name())
                .subCategory(t.getSubCategory())
                .amount(t.getAmount())
                .paymentMethod(t.getPaymentMethod().name())
                .note(t.getNote())
                .proofFilePath(t.getProofFilePath())
                .status(t.getStatus())
                .createdByUsername(t.getCreatedBy().getUsername())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .updatedByUsername(t.getUpdatedBy() != null ? t.getUpdatedBy().getUsername() : null)
                .build();
    }

    private boolean hasRole(User user, String role) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return role.equalsIgnoreCase(user.getRole().trim());
    }
}
