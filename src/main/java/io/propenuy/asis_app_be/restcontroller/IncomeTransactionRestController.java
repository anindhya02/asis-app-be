package io.propenuy.asis_app_be.restcontroller;

import io.propenuy.asis_app_be.model.IncomeTransaction;
import io.propenuy.asis_app_be.repository.IncomeTransactionRepository;
import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restdto.response.IncomeTransactionListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.IncomeTransactionResponseDTO;
import io.propenuy.asis_app_be.restservice.IncomeTransactionRestService;
import io.propenuy.asis_app_be.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/income-transactions")
@RequiredArgsConstructor
public class IncomeTransactionRestController {

    private final IncomeTransactionRestService incomeTransactionService;
    private final IncomeTransactionRepository incomeTransactionRepository;
    private final JwtUtils jwtUtils;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponseDTO<IncomeTransactionResponseDTO>> create(
            @RequestParam("transactionDate") String transactionDate,
            @RequestParam("category") String category,
            @RequestParam("sourceType") String sourceType,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam("amount") String amount,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "donorName", required = false) String donorName,
            @RequestParam("proofFile") MultipartFile proofFile
    ) {
        try {
            String currentUsername = jwtUtils.getCurrentUsername();
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        BaseResponseDTO.<IncomeTransactionResponseDTO>builder()
                                .status("error")
                                .message("Unauthorized - Silakan login terlebih dahulu")
                                .data(null)
                                .build()
                );
            }

            IncomeTransactionResponseDTO response = incomeTransactionService.create(
                    transactionDate,
                    category,
                    sourceType,
                    paymentMethod,
                    amount,
                    note,
                    donorName,
                    proofFile,
                    currentUsername
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    BaseResponseDTO.<IncomeTransactionResponseDTO>builder()
                            .status("success")
                            .message("Transaksi pemasukan berhasil dibuat")
                            .data(response)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<IncomeTransactionResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<IncomeTransactionResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    @GetMapping
    public ResponseEntity<BaseResponseDTO<IncomeTransactionListResponseDTO>> list(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(value = "sourceType", required = false) String sourceType,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) {
        try {
            IncomeTransactionListResponseDTO data = incomeTransactionService.list(
                    startDate,
                    endDate,
                    category,
                    paymentMethod,
                    sourceType,
                    search,
                    page,
                    size
            );

            return ResponseEntity.ok(
                    BaseResponseDTO.<IncomeTransactionListResponseDTO>builder()
                            .status("success")
                            .message("Income transactions retrieved successfully")
                            .data(data)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<IncomeTransactionListResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<IncomeTransactionListResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDTO<IncomeTransactionResponseDTO>> getDetail(
            @PathVariable UUID id
    ) {
        try {
            IncomeTransactionResponseDTO data = incomeTransactionService.getById(id);
            return ResponseEntity.ok(
                    BaseResponseDTO.<IncomeTransactionResponseDTO>builder()
                            .status("success")
                            .message("Income transaction detail retrieved successfully")
                            .data(data)
                            .build()
            );
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponseDTO.<IncomeTransactionResponseDTO>builder()
                            .status("error")
                            .message("Transaksi tidak ditemukan")
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<IncomeTransactionResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponseDTO<IncomeTransactionResponseDTO>> update(
            @PathVariable UUID id,
            @RequestParam("transactionDate") String transactionDate,
            @RequestParam("category") String category,
            @RequestParam("sourceType") String sourceType,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam("amount") String amount,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "donorName", required = false) String donorName,
            @RequestParam(value = "proofFile", required = false) MultipartFile proofFile
    ) {
        try {
            String currentUsername = jwtUtils.getCurrentUsername();
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        BaseResponseDTO.<IncomeTransactionResponseDTO>builder()
                                .status("error")
                                .message("Unauthorized - Silakan login terlebih dahulu")
                                .data(null)
                                .build()
                );
            }

            IncomeTransactionResponseDTO response = incomeTransactionService.update(
                    id,
                    transactionDate,
                    category,
                    sourceType,
                    paymentMethod,
                    amount,
                    note,
                    donorName,
                    proofFile,
                    currentUsername
            );

            return ResponseEntity.ok(
                    BaseResponseDTO.<IncomeTransactionResponseDTO>builder()
                            .status("success")
                            .message("Transaksi pemasukan berhasil diperbarui")
                            .data(response)
                            .build()
            );
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponseDTO.<IncomeTransactionResponseDTO>builder()
                            .status("error")
                            .message("Transaksi tidak ditemukan")
                            .data(null)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<IncomeTransactionResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<IncomeTransactionResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponseDTO<Void>> softDelete(@PathVariable UUID id) {
        try {
            String currentUsername = jwtUtils.getCurrentUsername();
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        BaseResponseDTO.<Void>builder()
                                .status("error")
                                .message("Unauthorized - Silakan login terlebih dahulu")
                                .data(null)
                                .build()
                );
            }

            incomeTransactionService.softDelete(id, currentUsername);

            return ResponseEntity.ok(
                    BaseResponseDTO.<Void>builder()
                            .status("success")
                            .message("Transaksi berhasil dinonaktifkan")
                            .data(null)
                            .build()
            );
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponseDTO.<Void>builder()
                            .status("error")
                            .message("Transaksi tidak ditemukan")
                            .data(null)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<Void>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<Void>builder()
                            .status("error")
                            .message("Terjadi kesalahan: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    @GetMapping("/{id}/proof")
    public ResponseEntity<?> getProofFile(@PathVariable UUID id) {
        IncomeTransaction transaction = incomeTransactionRepository.findById(id)
                .orElse(null);
        if (transaction == null
                || !"CONFIRMED".equals(transaction.getStatus())
                || transaction.getProofFilePath() == null
                || transaction.getProofFilePath().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        // proofFilePath sekarang berisi Cloudinary URL — redirect langsung
        String proofUrl = transaction.getProofFilePath();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(proofUrl))
                .build();
    }
}
