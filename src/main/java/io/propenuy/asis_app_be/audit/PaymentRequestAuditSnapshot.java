package io.propenuy.asis_app_be.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.propenuy.asis_app_be.model.PaymentRequest;
import io.propenuy.asis_app_be.model.PaymentRequestBreakdown;
import io.propenuy.asis_app_be.model.enums.ExpenseCategory;
import io.propenuy.asis_app_be.model.enums.PaymentMethod;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class PaymentRequestAuditSnapshot {

    private final ObjectMapper objectMapper;

    public PaymentRequestAuditSnapshot(@Qualifier("auditObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record BreakdownLine(String description, BigDecimal amount) {
    }

    public String toJson(PaymentRequestAuditRecorder.BeforeState state, Boolean supportingDocumentChanged, String reviewNote) {
        if (state == null) {
            return null;
        }
        return toCompactJson(
                state.title(),
                state.neededDate(),
                state.expenseCategory(),
                state.subCategory(),
                state.amount(),
                state.paymentMethod(),
                state.notes(),
                state.breakdowns(),
                supportingDocumentChanged,
                reviewNote);
    }

    public String toJson(PaymentRequest request, Boolean supportingDocumentChanged, String reviewNote) {
        if (request == null) {
            return null;
        }
        return toCompactJson(
                request.getTitle(),
                request.getNeededDate(),
                request.getExpenseCategory(),
                request.getSubCategory(),
                request.getAmount(),
                request.getPaymentMethod(),
                request.getNotes(),
                breakdownLinesFromEntity(request.getBreakdowns()),
                supportingDocumentChanged,
                reviewNote);
    }

    public static List<BreakdownLine> breakdownLinesFromEntity(List<PaymentRequestBreakdown> breakdowns) {
        if (breakdowns == null || breakdowns.isEmpty()) {
            return List.of();
        }
        List<BreakdownLine> lines = new ArrayList<>();
        for (PaymentRequestBreakdown b : breakdowns) {
            if (b == null) {
                continue;
            }
            lines.add(new BreakdownLine(b.getDescription(), b.getAmount()));
        }
        return lines;
    }

    private String toCompactJson(
            String title,
            LocalDate neededDate,
            ExpenseCategory expenseCategory,
            String subCategory,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String notes,
            List<BreakdownLine> breakdowns,
            Boolean supportingDocumentChanged,
            String reviewNote) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            if (title != null && !title.isBlank()) {
                node.put("title", title.trim());
            }
            if (neededDate != null) {
                node.put("neededDate", neededDate.toString());
            }
            if (expenseCategory != null) {
                node.put("expenseCategory", expenseCategory.name());
            }
            if (subCategory != null && !subCategory.isBlank()) {
                node.put("subCategory", subCategory.trim());
            }
            if (amount != null) {
                node.put("amount", amount);
            }
            if (paymentMethod != null) {
                node.put("paymentMethod", paymentMethod.name());
            }
            if (notes != null && !notes.isBlank()) {
                node.put("notes", notes.trim());
            }
            if (breakdowns != null && !breakdowns.isEmpty()) {
                ArrayNode arr = node.putArray("breakdowns");
                for (BreakdownLine line : breakdowns) {
                    ObjectNode item = arr.addObject();
                    item.put("description", line.description() != null ? line.description() : "");
                    if (line.amount() != null) {
                        item.put("amount", line.amount());
                    }
                }
            }
            if (supportingDocumentChanged != null) {
                node.put("supportingDocumentChanged", supportingDocumentChanged);
            }
            if (reviewNote != null && !reviewNote.isBlank()) {
                node.put("reviewNote", reviewNote.trim());
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }
}
