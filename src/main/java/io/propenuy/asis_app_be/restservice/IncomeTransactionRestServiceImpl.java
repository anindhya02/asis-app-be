package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.model.IncomeTransaction;
import io.propenuy.asis_app_be.model.User;
import io.propenuy.asis_app_be.model.enums.IncomeCategory;
import io.propenuy.asis_app_be.model.enums.PaymentMethod;
import io.propenuy.asis_app_be.repository.IncomeTransactionRepository;
import io.propenuy.asis_app_be.repository.UserRepository;
import io.propenuy.asis_app_be.restdto.response.IncomeTransactionResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncomeTransactionRestServiceImpl implements IncomeTransactionRestService {

    private final IncomeTransactionRepository incomeTransactionRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final List<String> ALLOWED_DOC_TYPES = List.of(
            "application/pdf"
    );

    @Value("${app.upload.dir:uploads/income-proofs}")
    private String uploadDir;

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

        // Save file
        String proofFilePath = saveProofFile(proofFile);

        IncomeTransaction transaction = IncomeTransaction.builder()
                .transactionDate(transactionDate)
                .category(incomeCategory)
                .sourceType(sourceType.trim())
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

    private String saveProofFile(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String filename = UUID.randomUUID() + extension;

            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            return uploadDir + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Gagal menyimpan file bukti: " + e.getMessage());
        }
    }

    private IncomeTransactionResponseDTO toResponseDTO(IncomeTransaction t) {
        return IncomeTransactionResponseDTO.builder()
                .id(t.getId())
                .transactionDate(t.getTransactionDate())
                .category(t.getCategory().name())
                .sourceType(t.getSourceType())
                .paymentMethod(t.getPaymentMethod().name())
                .amount(t.getAmount())
                .donorName(t.getDonorName())
                .note(t.getNote())
                .proofFilePath(t.getProofFilePath())
                .status(t.getStatus())
                .createdByUsername(t.getCreatedBy().getUsername())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
