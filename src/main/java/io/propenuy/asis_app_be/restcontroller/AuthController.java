package io.propenuy.asis_app_be.restcontroller;

import io.propenuy.asis_app_be.model.User;
import io.propenuy.asis_app_be.repository.UserRepository;
import io.propenuy.asis_app_be.restdto.request.LoginJwtRequestDTO;
import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restdto.response.LoginJwtResponseDTO;
import io.propenuy.asis_app_be.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<BaseResponseDTO<LoginJwtResponseDTO>> login(
            @RequestBody LoginJwtRequestDTO request) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> 
                            new IllegalArgumentException("User tidak ditemukan"));

            String token = jwtUtils.generateJwtToken(user);

            LoginJwtResponseDTO response = new LoginJwtResponseDTO(
                    token,
                    user.getUserId(),
                    user.getUsername(),
                    user.getNama(),
                    user.getRole()
            );

            return ResponseEntity.ok(
                    BaseResponseDTO.<LoginJwtResponseDTO>builder()
                            .status("success")
                            .message("Login berhasil")
                            .data(response)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<LoginJwtResponseDTO>builder()
                            .status("error")
                            .message("Username atau password salah")
                            .data(null)
                            .build()
            );
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponseDTO<Object>> logout() {
        return ResponseEntity.ok(
                BaseResponseDTO.builder()
                        .status("success")
                        .message("Logout berhasil. Silakan hapus token di client.")
                        .data(null)
                        .build()
        );
    }
}
