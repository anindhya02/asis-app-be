package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.model.*;
import io.propenuy.asis_app_be.repository.*;
import io.propenuy.asis_app_be.restdto.request.*;
import io.propenuy.asis_app_be.restdto.response.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserRestServiceImpl implements UserRestService {

    private final UserRepository userRepository;

    @Override
    public UserResponseDTO createUser(CreateUserRequestDTO request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("User sudah terdaftar");
        }

        if (request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password minimal 8 karakter");
        }
        
        if (request.getRole() == null) {
            throw new IllegalArgumentException("Role wajib diisi");
        }

        String roleInput = request.getRole().toUpperCase();

        if (!roleInput.equals("ADMIN") &&
            !roleInput.equals("KETUA YAYASAN") &&
            !roleInput.equals("PENGURUS") &&
            !roleInput.equals("DONATUR")) {

            throw new IllegalArgumentException("Role tidak valid");
        }

        User user = User.builder()
                .nama(request.getNama())
                .username(request.getUsername())
                .password(request.getPassword())
                .role(roleInput)
                .status("ACTIVE")
                .createdDate(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .nama(user.getNama())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .createdDate(user.getCreatedDate())
                .build();
    }

    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> UserResponseDTO.builder()
                        .userId(user.getUserId())
                        .nama(user.getNama())
                        .username(user.getUsername())
                        .role(user.getRole())
                        .status(user.getStatus())
                        .createdDate(user.getCreatedDate())
                        .build())
                .toList();
    }
}