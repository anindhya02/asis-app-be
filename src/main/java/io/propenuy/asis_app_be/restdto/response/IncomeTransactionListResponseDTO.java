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
public class IncomeTransactionListResponseDTO {
    private List<IncomeTransactionResponseDTO> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}

