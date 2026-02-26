package io.propenuy.asis_app_be.restdto.request;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginJwtRequestDTO {
    private String username;
    private String password;
}
