package io.propenuy.asis_app_be.restservice;
import io.propenuy.asis_app_be.restdto.request.CreateUserRequestDTO;
import io.propenuy.asis_app_be.restdto.request.UpdateUserRequestDTO;
import io.propenuy.asis_app_be.restdto.response.UserResponseDTO;
import java.util.List;
import java.util.UUID;

public interface UserRestService {
    UserResponseDTO createUser(CreateUserRequestDTO request);
    List<UserResponseDTO> getAllUsers();
    UserResponseDTO getUserById(UUID userId);
    UserResponseDTO updateUser(UUID userId, UpdateUserRequestDTO request);
    UserResponseDTO deactivateUser(UUID userId);
}