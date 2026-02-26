package io.propenuy.asis_app_be.restdto.response;
import java.util.UUID;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginJwtResponseDTO {
    private String token;
    private UUID userId;
    private String username;
    private String nama;
    private String role;
}
