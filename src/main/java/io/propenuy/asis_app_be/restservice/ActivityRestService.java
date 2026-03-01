package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.restdto.request.CreateActivityRequestDTO;
import io.propenuy.asis_app_be.restdto.request.UpdateActivityRequestDTO;
import io.propenuy.asis_app_be.restdto.response.ActivityResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ActivityRestService {

    ActivityResponseDTO createActivity(CreateActivityRequestDTO request, String currentUsername);

    List<ActivityResponseDTO> getAllActivities();

    ActivityResponseDTO getActivityById(UUID id);

    ActivityResponseDTO updateActivity(UUID id, UpdateActivityRequestDTO request);

    void deleteActivity(UUID id);
}
