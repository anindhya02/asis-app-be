package io.propenuy.asis_app_be.restdto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponseDTO {
    private UUID id;
    private String title;
    private String category;
    private String program;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private String status;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
