package io.propenuy.asis_app_be.restdto.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestListResponseDTO {
    private List<PaymentRequestResponseDTO> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
