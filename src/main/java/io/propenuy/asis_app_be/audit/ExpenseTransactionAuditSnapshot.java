package io.propenuy.asis_app_be.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.propenuy.asis_app_be.model.ExpenseTransaction;
import io.propenuy.asis_app_be.model.enums.ExpenseCategory;
import io.propenuy.asis_app_be.model.enums.PaymentMethod;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class ExpenseTransactionAuditSnapshot {

    private final ObjectMapper objectMapper;

    public ExpenseTransactionAuditSnapshot(@Qualifier("auditObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(ExpenseTransactionAuditRecorder.BeforeState state) {
        if (state == null) {
            return null;
        }
        return toCompactJson(
                state.transactionDate(),
                state.category(),
                state.subCategory(),
                state.paymentMethod(),
                state.amount(),
                state.note(),
                state.proofFilePath());
    }

    public String toJson(ExpenseTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        return toCompactJson(
                transaction.getTransactionDate(),
                transaction.getCategory(),
                transaction.getSubCategory(),
                transaction.getPaymentMethod(),
                transaction.getAmount(),
                transaction.getNote(),
                transaction.getProofFilePath());
    }

    private String toCompactJson(
            LocalDate transactionDate,
            ExpenseCategory category,
            String subCategory,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            String note,
            String proofFilePath) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            if (transactionDate != null) {
                node.put("transactionDate", transactionDate.toString());
            }
            if (category != null) {
                node.put("category", category.name());
            }
            if (subCategory != null && !subCategory.isBlank()) {
                node.put("subCategory", subCategory);
            }
            if (paymentMethod != null) {
                node.put("paymentMethod", paymentMethod.name());
            }
            if (amount != null) {
                node.put("amount", amount);
            }
            if (note != null && !note.isBlank()) {
                node.put("note", note);
            }
            if (proofFilePath != null && !proofFilePath.isBlank()) {
                node.put("proofFilePath", proofFilePath.trim());
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }
}
