package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.model.Activity;
import io.propenuy.asis_app_be.model.ActivityAttachment;
import io.propenuy.asis_app_be.repository.ActivityAttachmentRepository;
import io.propenuy.asis_app_be.repository.ActivityRepository;
import io.propenuy.asis_app_be.restdto.response.AttachmentResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityAttachmentRestServiceImpl implements ActivityAttachmentRestService {

    private final ActivityRepository activityRepository;
    private final ActivityAttachmentRepository attachmentRepository;
    private final CloudinaryStorageService cloudinaryStorageService;

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    @Transactional
    public List<AttachmentResponseDTO> uploadAttachments(UUID activityId, MultipartFile[] files) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Postingan kegiatan dengan id " + activityId + " tidak ditemukan"));

        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Minimal satu file harus diupload");
        }

        List<AttachmentResponseDTO> results = new ArrayList<>();

        for (MultipartFile file : files) {
            // Validasi tipe file
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
                throw new IllegalArgumentException(
                        "Tipe file tidak diizinkan: " + file.getOriginalFilename()
                                + ". Hanya JPG dan PNG yang diperbolehkan.");
            }

            // Validasi ukuran file
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException(
                        "Ukuran file terlalu besar: " + file.getOriginalFilename()
                                + ". Maksimal 5MB per file.");
            }

            // Generate unique storage path
            String originalFilename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : "unnamed";
            String storagePath = activityId + "/" + System.currentTimeMillis() + "-" + originalFilename;

            try {
                String fileUrl = cloudinaryStorageService.uploadFile(file, storagePath);

                ActivityAttachment attachment = ActivityAttachment.builder()
                        .activity(activity)
                        .fileName(originalFilename)
                        .fileType(contentType)
                        .fileSize(file.getSize())
                        .storagePath(storagePath)
                        .fileUrl(fileUrl)
                        .build();

                attachmentRepository.save(attachment);

                results.add(toResponseDTO(attachment));
            } catch (IOException e) {
                throw new RuntimeException("Gagal mengupload file: " + originalFilename + " - " + e.getMessage(), e);
            }
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponseDTO> getAttachmentsByActivityId(UUID activityId) {
        // Pastikan activity ada
        activityRepository.findById(activityId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Postingan kegiatan dengan id " + activityId + " tidak ditemukan"));

        return attachmentRepository.findAllByActivityId(activityId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAttachment(UUID attachmentId) {
        ActivityAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Attachment dengan id " + attachmentId + " tidak ditemukan"));

        try {
            cloudinaryStorageService.deleteFile(attachment.getStoragePath());
        } catch (IOException e) {
            throw new RuntimeException("Gagal menghapus file dari storage: " + e.getMessage(), e);
        }

        attachmentRepository.delete(attachment);
    }

    private AttachmentResponseDTO toResponseDTO(ActivityAttachment attachment) {
        return AttachmentResponseDTO.builder()
                .id(attachment.getId())
                .url(attachment.getFileUrl())
                .filename(attachment.getFileName())
                .type(attachment.getFileType())
                .size(attachment.getFileSize())
                .build();
    }
}
