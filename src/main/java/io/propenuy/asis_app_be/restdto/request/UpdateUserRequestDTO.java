package io.propenuy.asis_app_be.restdto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequestDTO {
    private String nama;
    private String username;
    private String password;
    private String role;
}
