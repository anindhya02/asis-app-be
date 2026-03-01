package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.model.Activity;
import io.propenuy.asis_app_be.model.User;
import io.propenuy.asis_app_be.model.enums.ActivityStatus;
import io.propenuy.asis_app_be.repository.ActivityRepository;
import io.propenuy.asis_app_be.repository.UserRepository;
import io.propenuy.asis_app_be.restdto.request.CreateActivityRequestDTO;
import io.propenuy.asis_app_be.restdto.request.UpdateActivityRequestDTO;
import io.propenuy.asis_app_be.restdto.response.ActivityResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityRestServiceImpl implements ActivityRestService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ActivityResponseDTO createActivity(CreateActivityRequestDTO request, String currentUsername) {
        // Validasi field wajib
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Judul kegiatan wajib diisi");
        }
        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw new IllegalArgumentException("Kategori kegiatan wajib diisi");
        }
        if (request.getProgram() == null || request.getProgram().isBlank()) {
            throw new IllegalArgumentException("Program kegiatan wajib diisi");
        }
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("Tanggal mulai kegiatan wajib diisi");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("Deskripsi/konten kegiatan wajib diisi");
        }

        // Get current user (Pengurus)
        User createdByUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        // Build entity dengan default status CREATED (draft)
        Activity activity = Activity.builder()
                .title(request.getTitle().trim())
                .category(request.getCategory().trim())
                .program(request.getProgram().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .description(request.getDescription().trim())
                .status(ActivityStatus.CREATED)
                .createdBy(createdByUser)
                .build();

        activityRepository.save(activity);

        return toResponseDTO(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponseDTO> getAllActivities() {
        return activityRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityResponseDTO getActivityById(UUID id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Postingan kegiatan dengan id " + id + " tidak ditemukan"));

        return toResponseDTO(activity);
    }

    @Override
    @Transactional
    public ActivityResponseDTO updateActivity(UUID id, UpdateActivityRequestDTO request) {
        // Validasi field wajib
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Judul kegiatan wajib diisi");
        }
        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw new IllegalArgumentException("Kategori kegiatan wajib diisi");
        }
        if (request.getProgram() == null || request.getProgram().isBlank()) {
            throw new IllegalArgumentException("Program kegiatan wajib diisi");
        }
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("Tanggal mulai kegiatan wajib diisi");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("Deskripsi/konten kegiatan wajib diisi");
        }

        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Postingan kegiatan dengan id " + id + " tidak ditemukan"));

        activity.setTitle(request.getTitle().trim());
        activity.setCategory(request.getCategory().trim());
        activity.setProgram(request.getProgram().trim());
        activity.setStartDate(request.getStartDate());
        activity.setEndDate(request.getEndDate());
        activity.setDescription(request.getDescription().trim());

        activityRepository.save(activity);

        return toResponseDTO(activity);
    }

    @Override
    @Transactional
    public void deleteActivity(UUID id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Postingan kegiatan dengan id " + id + " tidak ditemukan"));

        activityRepository.delete(activity);
    }

    private ActivityResponseDTO toResponseDTO(Activity activity) {
        return ActivityResponseDTO.builder()
                .id(activity.getId())
                .title(activity.getTitle())
                .category(activity.getCategory())
                .program(activity.getProgram())
                .startDate(activity.getStartDate())
                .endDate(activity.getEndDate())
                .description(activity.getDescription())
                .status(activity.getStatus().name())
                .createdByUsername(activity.getCreatedBy().getUsername())
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }
}
