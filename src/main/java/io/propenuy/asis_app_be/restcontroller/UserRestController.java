package io.propenuy.asis_app_be.restcontroller;
import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.restdto.response.UserResponseDTO;
import io.propenuy.asis_app_be.restdto.request.CreateUserRequestDTO;
import io.propenuy.asis_app_be.restservice.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserRestController {

    private final UserRestService userService;

    @PostMapping("/create")
    public ResponseEntity<BaseResponseDTO<UserResponseDTO>> createUser(
            @RequestBody CreateUserRequestDTO request) {

        try {
            UserResponseDTO response = userService.createUser(request);
            return ResponseEntity.ok(
                    BaseResponseDTO.<UserResponseDTO>builder()
                            .status("success")
                            .message("User created successfully")
                            .data(response)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                BaseResponseDTO.<UserResponseDTO>builder()
                        .status("error")
                        .message(e.getMessage())
                        .data(null)
                        .build()
            );
        }
    }

    @GetMapping
        public ResponseEntity<BaseResponseDTO<List<UserResponseDTO>>> getUsers() {
            try {
                return ResponseEntity.ok(
                    BaseResponseDTO.<List<UserResponseDTO>>builder()
                            .status("success")
                            .message("Users retrieved successfully")
                            .data(userService.getAllUsers())
                            .build());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(
                    BaseResponseDTO.<List<UserResponseDTO>>builder()
                            .status("error")
                            .message(e.getMessage())
                            .data(null)
                            .build()
                );
            }
        }
}
