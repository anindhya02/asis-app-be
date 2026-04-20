package io.propenuy.asis_app_be.restcontroller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.propenuy.asis_app_be.restdto.request.PaymentRequestReviewActionRequestDTO;
import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restdto.response.PaymentRequestDetailResponseDTO;
import io.propenuy.asis_app_be.restservice.PaymentRequestAccessForbiddenException;
import io.propenuy.asis_app_be.restservice.PaymentRequestRestService;
import io.propenuy.asis_app_be.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payment-requests-review")
@RequiredArgsConstructor
public class PaymentRequestReviewRestController {

    private final PaymentRequestRestService paymentRequestRestService;
    private final JwtUtils jwtUtils;

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDTO<PaymentRequestDetailResponseDTO>> getReviewDetailById(
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
                            .message("Detail review pengajuan dana berhasil diambil")
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

    @PatchMapping("/{id}/approve")
    public ResponseEntity<BaseResponseDTO<PaymentRequestDetailResponseDTO>> approve(
            @PathVariable("id") String id,
            @RequestBody(required = false) PaymentRequestReviewActionRequestDTO request
    ) {
        return handleReviewAction(
                id,
                request != null ? request.getReviewNote() : null,
                "Ticket disetujui"
        );
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<BaseResponseDTO<PaymentRequestDetailResponseDTO>> reject(
            @PathVariable("id") String id,
            @RequestBody(required = false) PaymentRequestReviewActionRequestDTO request
    ) {
        return handleReviewAction(
                id,
                request != null ? request.getReviewNote() : null,
                "Ticket ditolak",
                "reject"
        );
    }

    @PatchMapping("/{id}/request-revision")
    public ResponseEntity<BaseResponseDTO<PaymentRequestDetailResponseDTO>> requestRevision(
            @PathVariable("id") String id,
            @RequestBody(required = false) PaymentRequestReviewActionRequestDTO request
    ) {
        return handleReviewAction(
                id,
                request != null ? request.getReviewNote() : null,
                "Revisi diminta",
                "request_revision"
        );
    }

    private ResponseEntity<BaseResponseDTO<PaymentRequestDetailResponseDTO>> handleReviewAction(
            String id,
            String reviewNote,
            String successMessage
    ) {
        return handleReviewAction(id, reviewNote, successMessage, "approve");
    }

    private ResponseEntity<BaseResponseDTO<PaymentRequestDetailResponseDTO>> handleReviewAction(
            String id,
            String reviewNote,
            String successMessage,
            String action
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

            PaymentRequestDetailResponseDTO data;
            if ("reject".equals(action)) {
                data = paymentRequestRestService.reject(id, reviewNote, currentUsername);
            } else if ("request_revision".equals(action)) {
                data = paymentRequestRestService.requestRevision(id, reviewNote, currentUsername);
            } else {
                data = paymentRequestRestService.approve(id, reviewNote, currentUsername);
            }

            return ResponseEntity.ok(
                    BaseResponseDTO.<PaymentRequestDetailResponseDTO>builder()
                            .status("success")
                            .message(successMessage)
                            .data(data)
                            .build()
            );
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponseDTO.<PaymentRequestDetailResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
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
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
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
}
