package io.propenuy.asis_app_be.restdto.response;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private UUID userId;
    private String nama;
    private String username;
    private String role;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private String deletedBy;
}
