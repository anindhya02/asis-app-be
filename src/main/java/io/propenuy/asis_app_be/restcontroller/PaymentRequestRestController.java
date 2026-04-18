package io.propenuy.asis_app_be.restcontroller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restdto.response.PaymentRequestDetailResponseDTO;
import io.propenuy.asis_app_be.restdto.response.PaymentRequestListResponseDTO;
import io.propenuy.asis_app_be.restdto.response.PaymentRequestResponseDTO;
import io.propenuy.asis_app_be.restservice.PaymentRequestAccessForbiddenException;
import io.propenuy.asis_app_be.restservice.PaymentRequestRestService;
import io.propenuy.asis_app_be.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payment-requests")
@RequiredArgsConstructor
public class PaymentRequestRestController {

    private final PaymentRequestRestService paymentRequestRestService;
    private final JwtUtils jwtUtils;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponseDTO<PaymentRequestResponseDTO>> create(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "neededDate", required = false) String neededDate,
            @RequestParam(value = "expenseCategory", required = false) String expenseCategory,
            @RequestParam(value = "subCategory", required = false) String subCategory,
            @RequestParam(value = "amount", required = false) String amount,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "breakdownList", required = false) String breakdownList,
            @RequestParam(value = "submit", required = false) String submit,
            @RequestParam(value = "supportingDocument", required = false) MultipartFile supportingDocument
    ) {
        try {
            String currentUsername = jwtUtils.getCurrentUsername();
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        BaseResponseDTO.<PaymentRequestResponseDTO>builder()
                                .status("error")
                                .message("Unauthorized - Silakan login terlebih dahulu")
                                .data(null)
                                .build()
                );
            }

            PaymentRequestResponseDTO response = paymentRequestRestService.create(
                    title,
                    neededDate,
                    expenseCategory,
                    subCategory,
                    amount,
                    paymentMethod,
                    notes,
                    breakdownList,
                    submit,
                    supportingDocument,
                    currentUsername
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    BaseResponseDTO.<PaymentRequestResponseDTO>builder()
                            .status("success")
                            .message("Pengajuan dana berhasil dibuat")
                            .data(response)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<PaymentRequestResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<PaymentRequestResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    @PatchMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponseDTO<PaymentRequestResponseDTO>> update(
            @PathVariable("id") String id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "neededDate", required = false) String neededDate,
            @RequestParam(value = "expenseCategory", required = false) String expenseCategory,
            @RequestParam(value = "subCategory", required = false) String subCategory,
            @RequestParam(value = "amount", required = false) String amount,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "breakdownList", required = false) String breakdownList,
            @RequestParam(value = "submit", required = false) String submit,
            @RequestParam(value = "supportingDocument", required = false) MultipartFile supportingDocument
    ) {
        try {
            String currentUsername = jwtUtils.getCurrentUsername();
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        BaseResponseDTO.<PaymentRequestResponseDTO>builder()
                                .status("error")
                                .message("Unauthorized - Silakan login terlebih dahulu")
                                .data(null)
                                .build()
                );
            }

            PaymentRequestResponseDTO response = paymentRequestRestService.update(
                    id,
                    title,
                    neededDate,
                    expenseCategory,
                    subCategory,
                    amount,
                    paymentMethod,
                    notes,
                    breakdownList,
                    submit,
                    supportingDocument,
                    currentUsername
            );

            if (response == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        BaseResponseDTO.<PaymentRequestResponseDTO>builder()
                                .status("error")
                                .message("Ticket tidak ditemukan")
                                .data(null)
                                .build()
                );
            }

            return ResponseEntity.ok(
                    BaseResponseDTO.<PaymentRequestResponseDTO>builder()
                            .status("success")
                            .message("Pengajuan dana berhasil diperbarui")
                            .data(response)
                            .build()
            );
        } catch (PaymentRequestAccessForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    BaseResponseDTO.<PaymentRequestResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<PaymentRequestResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<PaymentRequestResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponseDTO<Void>> cancel(@PathVariable("id") String id) {
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

            paymentRequestRestService.cancel(id, currentUsername);

            return ResponseEntity.ok(
                    BaseResponseDTO.<Void>builder()
                            .status("success")
                            .message("Ticket berhasil dibatalkan")
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
        } catch (PaymentRequestAccessForbiddenException e) {
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

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDTO<PaymentRequestDetailResponseDTO>> getById(
            @PathVariable("id") String id
    ) {
        try {
            String currentUsername = jwtUtils.getCurrentUsername();
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        BaseResponseDTO.<PaymentRequestDetailResponseDTO>builder()
                                .status("error")
                                .message("Unauthorized - Silakan login terlebih dahulu")
                                .data(null)
                                .build()
                );
            }

            PaymentRequestDetailResponseDTO data = paymentRequestRestService.getById(id, currentUsername);

            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        BaseResponseDTO.<PaymentRequestDetailResponseDTO>builder()
                                .status("error")
                                .message("Ticket tidak ditemukan")
                                .data(null)
                                .build()
                );
            }

            return ResponseEntity.ok(
                    BaseResponseDTO.<PaymentRequestDetailResponseDTO>builder()
                            .status("success")
                            .message("Detail pengajuan dana berhasil diambil")
                            .data(data)
                            .build()
            );
        } catch (PaymentRequestAccessForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    BaseResponseDTO.<PaymentRequestDetailResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<PaymentRequestDetailResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<PaymentRequestDetailResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

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
