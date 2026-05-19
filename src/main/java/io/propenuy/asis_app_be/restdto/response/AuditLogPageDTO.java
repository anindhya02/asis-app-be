package io.propenuy.asis_app_be.restdto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogPageDTO {
    private List<AuditLogRowDTO> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;
}
