package io.propenuy.asis_app_be.restdto.response;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponseDTO {
    private UUID id;
    private String url;
    private String filename;
    private String type;
    private Long size;
}
