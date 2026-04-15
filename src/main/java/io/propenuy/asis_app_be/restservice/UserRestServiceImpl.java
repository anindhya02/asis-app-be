package io.propenuy.asis_app_be.restservice;

import io.propenuy.asis_app_be.model.*;
import io.propenuy.asis_app_be.repository.*;
import io.propenuy.asis_app_be.restdto.request.*;
import io.propenuy.asis_app_be.restdto.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserRestServiceImpl implements UserRestService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder;

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
                .password(passwordEncoder.encode(request.getPassword()))
                .role(roleInput)
                .status("ACTIVE")
                .createdDate(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return mapToDTO(user);
    }

    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public UserResponseDTO getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        return mapToDTO(user);
    }

    @Override
    public UserResponseDTO updateUser(UUID userId, UpdateUserRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        if (request.getNama() == null || request.getNama().isBlank()) {
            throw new IllegalArgumentException("Nama wajib diisi");
        }

        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username wajib diisi");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password wajib diisi");
        }

        if (request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password minimal 8 karakter");
        }

        if (request.getRole() == null || request.getRole().isBlank()) {
            throw new IllegalArgumentException("Role wajib diisi");
        }

        String roleInput = request.getRole().trim().toUpperCase();
        if (!roleInput.equals("ADMIN") &&
            !roleInput.equals("KETUA YAYASAN") &&
            !roleInput.equals("PENGURUS") &&
            !roleInput.equals("DONATUR")) {
            throw new IllegalArgumentException("Role tidak valid");
        }

        String usernameInput = request.getUsername().trim();
        if (userRepository.existsByUsernameAndUserIdNot(usernameInput, userId)) {
            throw new IllegalArgumentException("User sudah terdaftar");
        }

        user.setNama(request.getNama().trim());
        user.setUsername(usernameInput);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(roleInput);

        userRepository.save(user);
        return mapToDTO(user);
    }

    @Override
    public UserResponseDTO deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        user.setStatus("INACTIVE");
        userRepository.save(user);

        return mapToDTO(user);
    }

    private UserResponseDTO mapToDTO(User user) {
        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .nama(user.getNama())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .createdDate(user.getCreatedDate())
                .build();
    }
}