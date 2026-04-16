package io.propenuy.asis_app_be.restcontroller;

import io.propenuy.asis_app_be.restdto.request.CreateReplyRequestDTO;
import io.propenuy.asis_app_be.restdto.request.UpdateReplyRequestDTO;
import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restdto.response.ReplyResponseDTO;
import io.propenuy.asis_app_be.restservice.ReplyRestService;
import io.propenuy.asis_app_be.security.jwt.JwtUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReplyRestController {

    private final ReplyRestService replyRestService;
    private final JwtUtils jwtUtils;

    @PostMapping("/api/activities/{activityId}/replies")
    public ResponseEntity<BaseResponseDTO<ReplyResponseDTO>> createReply(
            @PathVariable UUID activityId,
            @RequestBody CreateReplyRequestDTO request
    ) {
        try {
            String currentUsername = jwtUtils.getCurrentUsername();
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        BaseResponseDTO.<ReplyResponseDTO>builder()
                                .status("error")
                                .message("Unauthorized - Silakan login terlebih dahulu")
                                .data(null)
                                .build()
                );
            }

            ReplyResponseDTO response = replyRestService.createReply(activityId, request, currentUsername);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    BaseResponseDTO.<ReplyResponseDTO>builder()
                            .status("success")
                            .message("Reply berhasil dibuat")
                            .data(response)
                            .build()
            );
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponseDTO.<ReplyResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<ReplyResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<ReplyResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan saat membuat reply")
                            .data(null)
                            .build()
            );
        }
    }

    @GetMapping("/api/activities/{activityId}/replies")
    public ResponseEntity<BaseResponseDTO<List<ReplyResponseDTO>>> getReplies(
            @PathVariable UUID activityId
    ) {
        try {
            String currentUsername = jwtUtils.getCurrentUsername();
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        BaseResponseDTO.<List<ReplyResponseDTO>>builder()
                                .status("error")
                                .message("Unauthorized - Silakan login terlebih dahulu")
                                .data(null)
                                .build()
                );
            }

            List<ReplyResponseDTO> replies = replyRestService.getRepliesByActivityId(activityId);

            return ResponseEntity.ok(
                    BaseResponseDTO.<List<ReplyResponseDTO>>builder()
                            .status("success")
                            .message("Berhasil mengambil daftar reply")
                            .data(replies)
                            .build()
            );
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponseDTO.<List<ReplyResponseDTO>>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<List<ReplyResponseDTO>>builder()
                            .status("error")
                            .message("Terjadi kesalahan saat mengambil daftar reply")
                            .data(null)
                            .build()
            );
        }
    }

    @PutMapping("/api/replies/{replyId}")
    public ResponseEntity<BaseResponseDTO<ReplyResponseDTO>> updateReply(
            @PathVariable UUID replyId,
            @RequestBody UpdateReplyRequestDTO request
    ) {
        try {
            String currentUsername = jwtUtils.getCurrentUsername();
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        BaseResponseDTO.<ReplyResponseDTO>builder()
                                .status("error")
                                .message("Unauthorized - Silakan login terlebih dahulu")
                                .data(null)
                                .build()
                );
            }

            ReplyResponseDTO response = replyRestService.updateReply(replyId, request, currentUsername);

            return ResponseEntity.ok(
                    BaseResponseDTO.<ReplyResponseDTO>builder()
                            .status("success")
                            .message("Reply berhasil diperbarui")
                            .data(response)
                            .build()
            );
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponseDTO.<ReplyResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    BaseResponseDTO.<ReplyResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<ReplyResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<ReplyResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan saat memperbarui reply")
                            .data(null)
                            .build()
            );
        }
    }

    @DeleteMapping("/api/replies/{replyId}")
    public ResponseEntity<?> deleteReply(@PathVariable UUID replyId) {
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

            replyRestService.deleteReply(replyId, currentUsername);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponseDTO.<Void>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
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
                            .message("Terjadi kesalahan saat menghapus reply")
                            .data(null)
                            .build()
            );
        }
    }
}
