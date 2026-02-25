package io.propenuy.asis_app_be.restdto.request;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequestDTO {
    private String nama;
    private String username;
    private String password;
    private String role;
}
