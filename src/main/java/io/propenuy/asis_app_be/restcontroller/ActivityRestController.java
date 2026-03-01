package io.propenuy.asis_app_be.restcontroller;

import io.propenuy.asis_app_be.restdto.request.CreateActivityRequestDTO;
import io.propenuy.asis_app_be.restdto.request.UpdateActivityRequestDTO;
import io.propenuy.asis_app_be.restdto.response.ActivityResponseDTO;
import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restservice.ActivityRestService;
import io.propenuy.asis_app_be.security.jwt.JwtUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityRestController {

    private final ActivityRestService activityRestService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ResponseEntity<BaseResponseDTO<ActivityResponseDTO>> createActivity(
            @RequestBody CreateActivityRequestDTO request
    ) {
        try {
            String currentUsername = jwtUtils.getCurrentUsername();
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        BaseResponseDTO.<ActivityResponseDTO>builder()
                                .status("error")
                                .message("Unauthorized - Silakan login terlebih dahulu")
                                .data(null)
                                .build()
                );
            }

            ActivityResponseDTO response = activityRestService.createActivity(request, currentUsername);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    BaseResponseDTO.<ActivityResponseDTO>builder()
                            .status("success")
                            .message("Postingan kegiatan berhasil dibuat")
                            .data(response)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<ActivityResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<ActivityResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan saat membuat postingan kegiatan")
                            .data(null)
                            .build()
            );
        }
    }

    @GetMapping
    public ResponseEntity<BaseResponseDTO<List<ActivityResponseDTO>>> getAllActivities() {
        try {
            List<ActivityResponseDTO> activities = activityRestService.getAllActivities();

            return ResponseEntity.ok(
                    BaseResponseDTO.<List<ActivityResponseDTO>>builder()
                            .status("success")
                            .message("Berhasil mengambil semua postingan kegiatan")
                            .data(activities)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<List<ActivityResponseDTO>>builder()
                            .status("error")
                            .message("Terjadi kesalahan saat mengambil postingan kegiatan")
                            .data(null)
                            .build()
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponseDTO<ActivityResponseDTO>> getActivityById(@PathVariable UUID id) {
        try {
            ActivityResponseDTO response = activityRestService.getActivityById(id);

            return ResponseEntity.ok(
                    BaseResponseDTO.<ActivityResponseDTO>builder()
                            .status("success")
                            .message("Berhasil mengambil postingan kegiatan")
                            .data(response)
                            .build()
            );
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponseDTO.<ActivityResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<ActivityResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan saat mengambil postingan kegiatan")
                            .data(null)
                            .build()
            );
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponseDTO<ActivityResponseDTO>> updateActivity(
            @PathVariable UUID id,
            @RequestBody UpdateActivityRequestDTO request
    ) {
        try {
            String currentUsername = jwtUtils.getCurrentUsername();
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        BaseResponseDTO.<ActivityResponseDTO>builder()
                                .status("error")
                                .message("Unauthorized - Silakan login terlebih dahulu")
                                .data(null)
                                .build()
                );
            }

            ActivityResponseDTO response = activityRestService.updateActivity(id, request);

            return ResponseEntity.ok(
                    BaseResponseDTO.<ActivityResponseDTO>builder()
                            .status("success")
                            .message("Postingan kegiatan berhasil diperbarui")
                            .data(response)
                            .build()
            );
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponseDTO.<ActivityResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<ActivityResponseDTO>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponseDTO.<ActivityResponseDTO>builder()
                            .status("error")
                            .message("Terjadi kesalahan saat memperbarui postingan kegiatan")
                            .data(null)
                            .build()
            );
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponseDTO<Void>> deleteActivity(@PathVariable UUID id) {
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

            activityRestService.deleteActivity(id);

            return ResponseEntity.ok(
                    BaseResponseDTO.<Void>builder()
                            .status("success")
                            .message("Postingan kegiatan dengan id " + id + " berhasil dihapus")
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
                            .message("Terjadi kesalahan saat menghapus postingan kegiatan")
                            .data(null)
                            .build()
            );
        }
    }
}
