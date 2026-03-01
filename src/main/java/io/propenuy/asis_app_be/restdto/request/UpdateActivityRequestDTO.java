package io.propenuy.asis_app_be.restdto.request;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateActivityRequestDTO {

    private String title;

    private String category;

    private String program;

    private LocalDate startDate;

    private LocalDate endDate;

    private String description;
}
