package io.propenuy.asis_app_be.restservice;
import io.propenuy.asis_app_be.restdto.request.CreateUserRequestDTO;
import io.propenuy.asis_app_be.restdto.response.UserResponseDTO;
import java.util.List;

public interface UserRestService {
    UserResponseDTO createUser(CreateUserRequestDTO request);
    List<UserResponseDTO> getAllUsers();
}