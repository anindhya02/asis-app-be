package io.propenuy.asis_app_be.restcontroller;

import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restdto.response.PaymentRequestListResponseDTO;
import io.propenuy.asis_app_be.restservice.PaymentRequestRestService;
import io.propenuy.asis_app_be.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment-requests")
@RequiredArgsConstructor
public class PaymentRequestRestController {

    private final PaymentRequestRestService paymentRequestRestService;
    private final JwtUtils jwtUtils;

    @GetMapping
    public ResponseEntity<BaseResponseDTO<PaymentRequestListResponseDTO>> list(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "expenseCategory", required = false) String expenseCategory,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) {
        try {
            String currentUsername = jwtUtils.getCurrentUsername();
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        BaseResponseDTO.<PaymentRequestListResponseDTO>builder()
                                .status("error")
                                .message("Unauthorized - Silakan login terlebih dahulu")
                                .data(null)
                                .build()
                );
            }

            PaymentRequestListResponseDTO data = paymentRequestRestService.list(
                    startDate,
                    endDate,
                    status,
                    expenseCategory,
                    search,
                    page,
                    size,
                    currentUsername
            );

            return ResponseEntity.ok(
                    BaseResponseDTO.<PaymentRequestListResponseDTO>builder()
                            .status("success")
                            .message("Daftar pengajuan dana berhasil diambil")
                            .data(data)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<PaymentRequestListResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<PaymentRequestListResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }
}
