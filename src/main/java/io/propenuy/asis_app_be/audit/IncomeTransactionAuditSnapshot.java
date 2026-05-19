package io.propenuy.asis_app_be.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.propenuy.asis_app_be.model.IncomeTransaction;
import io.propenuy.asis_app_be.model.enums.IncomeCategory;
import io.propenuy.asis_app_be.model.enums.PaymentMethod;
import io.propenuy.asis_app_be.model.enums.SourceType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class IncomeTransactionAuditSnapshot {

    private final ObjectMapper objectMapper;

    public IncomeTransactionAuditSnapshot(@Qualifier("auditObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(IncomeTransactionAuditRecorder.BeforeState state, Boolean proofChanged) {
        if (state == null) {
            return null;
        }
        return toCompactJson(
                state.transactionDate(),
                state.category(),
                state.sourceType(),
                state.paymentMethod(),
                state.donorName(),
                state.amount(),
                state.note(),
                proofChanged);
    }

    public String toJson(IncomeTransaction transaction, Boolean proofChanged) {
        if (transaction == null) {
            return null;
        }
        return toCompactJson(
                transaction.getTransactionDate(),
                transaction.getCategory(),
                transaction.getSourceType(),
                transaction.getPaymentMethod(),
                transaction.getDonorName(),
                transaction.getAmount(),
                transaction.getNote(),
                proofChanged);
    }

    private String toCompactJson(
            LocalDate transactionDate,
            IncomeCategory category,
            SourceType sourceType,
            PaymentMethod paymentMethod,
            String donorName,
            BigDecimal amount,
            String note,
            Boolean proofChanged) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            if (transactionDate != null) {
                node.put("transactionDate", transactionDate.toString());
            }
            if (category != null) {
                node.put("category", category.name());
            }
            if (sourceType != null) {
                node.put("sourceType", sourceType.name());
            }
            if (paymentMethod != null) {
                node.put("paymentMethod", paymentMethod.name());
            }
            if (donorName != null && !donorName.isBlank()) {
                node.put("donorName", donorName);
            }
            if (amount != null) {
                node.put("amount", amount);
            }
            if (note != null && !note.isBlank()) {
                node.put("note", note);
            }
            if (proofChanged != null) {
                node.put("proofChanged", proofChanged);
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }
}
