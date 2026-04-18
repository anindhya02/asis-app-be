package io.propenuy.asis_app_be.restcontroller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.propenuy.asis_app_be.model.ExpenseTransaction;
import io.propenuy.asis_app_be.repository.ExpenseTransactionRepository;
import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restdto.response.ExpenseTransactionListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.ExpenseTransactionResponseDTO;
import io.propenuy.asis_app_be.restservice.ExpenseEditForbiddenException;
import io.propenuy.asis_app_be.restservice.ExpenseTransactionRestService;
import io.propenuy.asis_app_be.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/expense-transactions")
@RequiredArgsConstructor
public class ExpenseTransactionRestController {

    private final ExpenseTransactionRestService expenseTransactionService;
    private final ExpenseTransactionRepository expenseTransactionRepository;
    private final JwtUtils jwtUtils;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponseDTO<ExpenseTransactionResponseDTO>> create(
            @RequestParam("transactionDate") String transactionDate,
            @RequestParam("category") String category,
            @RequestParam(value = "subCategory", required = false) String subCategory,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam("amount") String amount,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam("proofFile") MultipartFile proofFile
    ) {
        try {
            String currentUsername = jwtUtils.getCurrentUsername();
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        BaseResponseDTO.<ExpenseTransactionResponseDTO>builder()
                                .status("error")
                                .message("Unauthorized - Silakan login terlebih dahulu")
                                .data(null)
                                .build()
                );
            }

            ExpenseTransactionResponseDTO response = expenseTransactionService.create(
                    transactionDate,
                    category,
                    subCategory,
                    paymentMethod,
                    amount,
                    note,
                    proofFile,
                    currentUsername
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    BaseResponseDTO.<ExpenseTransactionResponseDTO>builder()
                            .status("success")
                            .message("Transaksi pengeluaran berhasil dibuat")
                            .data(response)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<ExpenseTransactionResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<ExpenseTransactionResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    @GetMapping
    public ResponseEntity<BaseResponseDTO<ExpenseTransactionListResponseDTO>> list(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) {
        try {
            ExpenseTransactionListResponseDTO data = expenseTransactionService.list(
                    startDate,
                    endDate,
                    category,
                    paymentMethod,
                    search,
                    page,
                    size
            );

            return ResponseEntity.ok(
                    BaseResponseDTO.<ExpenseTransactionListResponseDTO>builder()
                            .status("success")
                            .message("Daftar transaksi pengeluaran berhasil diambil")
                            .data(data)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<ExpenseTransactionListResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<ExpenseTransactionListResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDTO<ExpenseTransactionResponseDTO>> getById(
            @PathVariable("id") String id
    ) {
        try {
            ExpenseTransactionResponseDTO data = expenseTransactionService.getById(id);

            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        BaseResponseDTO.<ExpenseTransactionResponseDTO>builder()
                                .status("error")
                                .message("Transaksi pengeluaran tidak ditemukan")
                                .data(null)
                                .build()
                );
            }

            return ResponseEntity.ok(
                    BaseResponseDTO.<ExpenseTransactionResponseDTO>builder()
                            .status("success")
                            .message("Detail transaksi pengeluaran berhasil diambil")
                            .data(data)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<ExpenseTransactionResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<ExpenseTransactionResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponseDTO<ExpenseTransactionResponseDTO>> update(
            @PathVariable("id") String id,
            @RequestParam("category") String category,
            @RequestParam(value = "subCategory", required = false) String subCategory,
            @RequestParam(value = "note", required = false) String note
    ) {
        try {
            String currentUsername = jwtUtils.getCurrentUsername();
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        BaseResponseDTO.<ExpenseTransactionResponseDTO>builder()
                                .status("error")
                                .message("Unauthorized - Silakan login terlebih dahulu")
                                .data(null)
                                .build()
                );
            }

            ExpenseTransactionResponseDTO response = expenseTransactionService.update(
                    id,
                    category,
                    subCategory,
                    note,
                    currentUsername
            );

            return ResponseEntity.ok(
                    BaseResponseDTO.<ExpenseTransactionResponseDTO>builder()
                            .status("success")
                            .message("Transaksi berhasil diperbarui")
                            .data(response)
                            .build()
            );
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponseDTO.<ExpenseTransactionResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (ExpenseEditForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    BaseResponseDTO.<ExpenseTransactionResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    BaseResponseDTO.<ExpenseTransactionResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<ExpenseTransactionResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<ExpenseTransactionResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponseDTO<Void>> softDelete(@PathVariable("id") String id) {
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

            expenseTransactionService.softDelete(id, currentUsername);
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
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (ExpenseEditForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    BaseResponseDTO.<Void>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    BaseResponseDTO.<Void>builder()
                            .status("error")
                            .message(e.getMessage())
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
        ExpenseTransaction transaction = expenseTransactionRepository.findById(id)
                .orElse(null);
        if (transaction == null || transaction.getProofFilePath() == null
                || transaction.getProofFilePath().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        // proofFilePath berisi Cloudinary URL — redirect langsung
        String proofUrl = transaction.getProofFilePath();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(proofUrl))
                .build();
    }
}
