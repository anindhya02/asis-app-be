package io.propenuy.asis_app_be.restcontroller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restdto.response.ExpenseTransactionListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.ExpenseTransactionResponseDTO;
import io.propenuy.asis_app_be.restservice.ExpenseTransactionRestService;
import io.propenuy.asis_app_be.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/expense-transactions")
@RequiredArgsConstructor
public class ExpenseTransactionRestController {

    private final ExpenseTransactionRestService expenseTransactionService;
    private final JwtUtils jwtUtils;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponseDTO<ExpenseTransactionResponseDTO>> create(
            @RequestParam("transactionDate") String transactionDate,
            @RequestParam("category") String category,
            @RequestParam("program") String program,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam("amount") String amount,
            @RequestParam("penerimaDana") String penerimaDana,
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
                    program,
                    paymentMethod,
                    amount,
                    penerimaDana,
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
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) {
        try {
            ExpenseTransactionListResponseDTO data = expenseTransactionService.list(
                    startDate,
                    endDate,
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
}
