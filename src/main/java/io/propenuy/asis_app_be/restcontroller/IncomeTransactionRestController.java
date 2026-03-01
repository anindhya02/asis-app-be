package io.propenuy.asis_app_be.restcontroller;

import io.propenuy.asis_app_be.model.IncomeTransaction;
import io.propenuy.asis_app_be.repository.IncomeTransactionRepository;
import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restdto.response.IncomeTransactionListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.IncomeTransactionResponseDTO;
import io.propenuy.asis_app_be.restservice.IncomeTransactionRestService;
import io.propenuy.asis_app_be.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @GetMapping("/{id}/proof")
    public ResponseEntity<Resource> getProofFile(@PathVariable UUID id) {
        IncomeTransaction transaction = incomeTransactionRepository.findById(id)
                .orElse(null);
        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path filePath = Paths.get(transaction.getProofFilePath()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = "application/octet-stream";
            String filename = filePath.getFileName().toString();
            if (filename.toLowerCase().endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (filename.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
                contentType = "image/" + filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
