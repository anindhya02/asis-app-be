package io.propenuy.asis_app_be.audit;

import io.propenuy.asis_app_be.model.PaymentRequest;
import io.propenuy.asis_app_be.model.enums.AuditActionType;
import io.propenuy.asis_app_be.model.enums.AuditModuleCode;
import io.propenuy.asis_app_be.model.enums.ExpenseCategory;
import io.propenuy.asis_app_be.model.enums.PaymentMethod;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentRequestAuditRecorder {

    private final AuditLogWriter auditLogWriter;
    private final PaymentRequestAuditSnapshot snapshot;

    public PaymentRequestAuditRecorder(AuditLogWriter auditLogWriter, PaymentRequestAuditSnapshot snapshot) {
        this.auditLogWriter = auditLogWriter;
        this.snapshot = snapshot;
    }

    public record BeforeState(
            String title,
            LocalDate neededDate,
            ExpenseCategory expenseCategory,
            String subCategory,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String notes,
            List<PaymentRequestAuditSnapshot.BreakdownLine> breakdowns,
            String supportingDocumentUrl) {
    }

    public BeforeState capture(PaymentRequest request) {
        return new BeforeState(
                request.getTitle(),
                request.getNeededDate(),
                request.getExpenseCategory(),
                request.getSubCategory(),
                request.getAmount(),
                request.getPaymentMethod(),
                request.getNotes(),
                PaymentRequestAuditSnapshot.breakdownLinesFromEntity(request.getBreakdowns()),
                request.getSupportingDocumentUrl());
    }

    public void recordAfterCreate(PaymentRequest request) {
        persist(request.getId(), AuditActionType.CREATE, null, snapshot.toJson(request, null, null));
    }

    public void recordAfterUpdate(BeforeState before, PaymentRequest after, boolean supportingDocumentChanged) {
        persist(
                after.getId(),
                AuditActionType.UPDATE,
                snapshot.toJson(before, null, null),
                snapshot.toJson(after, supportingDocumentChanged, null));
    }

    public void recordAfterSubmit(PaymentRequest request) {
        persist(
                request.getId(),
                AuditActionType.SUBMIT,
                null,
                snapshot.toJson(request, null, null));
    }

    public void recordAfterCancel(BeforeState before, UUID paymentRequestId) {
        persist(
                paymentRequestId,
                AuditActionType.CANCEL,
                snapshot.toJson(before, null, null),
                null);
    }

    public void recordAfterApprove(BeforeState before, PaymentRequest after, String reviewNote) {
        persist(
                after.getId(),
                AuditActionType.APPROVE,
                snapshot.toJson(before, null, null),
                snapshot.toJson(after, null, reviewNote));
    }

    public void recordAfterReject(BeforeState before, PaymentRequest after, String reviewNote) {
        persist(
                after.getId(),
                AuditActionType.REJECT,
                snapshot.toJson(before, null, null),
                snapshot.toJson(after, null, reviewNote));
    }

    public void recordAfterRevision(BeforeState before, PaymentRequest after, String reviewNote) {
        persist(
                after.getId(),
                AuditActionType.REVIEW,
                snapshot.toJson(before, null, null),
                snapshot.toJson(after, null, reviewNote));
    }

    private void persist(UUID id, AuditActionType actionType, String oldJson, String newJson) {
        auditLogWriter.persist(
                AuditModuleCode.PAYMENT_REQUEST,
                PaymentRequest.class,
                id,
                actionType,
                oldJson,
                newJson);
    }
}
