package io.propenuy.asis_app_be.restdto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpenseTransactionRequestDTO {
    private LocalDate transactionDate;
    private String category;
    private String subCategory;
    private BigDecimal amount;
    private String paymentMethod;
    private String note;
}
