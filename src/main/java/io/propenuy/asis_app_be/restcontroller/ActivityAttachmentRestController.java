package io.propenuy.asis_app_be.restcontroller;

import io.propenuy.asis_app_be.restdto.response.AttachmentResponseDTO;
import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restservice.ActivityAttachmentRestService;
import io.propenuy.asis_app_be.security.jwt.JwtUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/activities/{activityId}/attachments")
@RequiredArgsConstructor
public class ActivityAttachmentRestController {

    private final ActivityAttachmentRestService attachmentRestService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ResponseEntity<BaseResponseDTO<List<AttachmentResponseDTO>>> uploadAttachments(
            @PathVariable UUID activityId,
            @RequestParam("files") MultipartFile[] files
    ) {
        try {
            String currentUsername = jwtUtils.getCurrentUsername();
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        BaseResponseDTO.<List<AttachmentResponseDTO>>builder()
                                .status("error")
                                .message("Unauthorized - Silakan login terlebih dahulu")
                                .data(null)
                                .build()
                );
            }

            List<AttachmentResponseDTO> attachments = attachmentRestService.uploadAttachments(activityId, files);

            return ResponseEntity.ok(
                    BaseResponseDTO.<List<AttachmentResponseDTO>>builder()
                            .status("success")
                            .message("Berhasil mengupload " + attachments.size() + " lampiran")
                            .data(attachments)
                            .build()
            );
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponseDTO.<List<AttachmentResponseDTO>>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<List<AttachmentResponseDTO>>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<List<AttachmentResponseDTO>>builder()
                            .status("error")
                            .message("Terjadi kesalahan saat mengupload lampiran: " + e.getMessage())
                            .data(null)
                            .build()
            );
        }
    }

    @GetMapping
    public ResponseEntity<BaseResponseDTO<List<AttachmentResponseDTO>>> getAttachments(
            @PathVariable UUID activityId
    ) {
        try {
            List<AttachmentResponseDTO> attachments = attachmentRestService.getAttachmentsByActivityId(activityId);

            return ResponseEntity.ok(
                    BaseResponseDTO.<List<AttachmentResponseDTO>>builder()
                            .status("success")
                            .message("Berhasil mengambil lampiran")
                            .data(attachments)
                            .build()
            );
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponseDTO.<List<AttachmentResponseDTO>>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<List<AttachmentResponseDTO>>builder()
                            .status("error")
                            .message("Terjadi kesalahan saat mengambil lampiran")
                            .data(null)
                            .build()
            );
        }
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<BaseResponseDTO<Void>> deleteAttachment(
            @PathVariable UUID activityId,
            @PathVariable UUID attachmentId
    ) {
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

            attachmentRestService.deleteAttachment(attachmentId);

            return ResponseEntity.ok(
                    BaseResponseDTO.<Void>builder()
                            .status("success")
                            .message("Lampiran berhasil dihapus")
                            .data(null)
                            .build()
            );
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
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
                            .message("Terjadi kesalahan saat menghapus lampiran")
                            .data(null)
                            .build()
            );
        }
    }
}
