package io.propenuy.asis_app_be.restdto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseTransactionListResponseDTO {
    private List<ExpenseTransactionResponseDTO> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
