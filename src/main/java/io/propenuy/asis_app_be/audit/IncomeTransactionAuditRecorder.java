package io.propenuy.asis_app_be.audit;

import io.propenuy.asis_app_be.model.IncomeTransaction;
import io.propenuy.asis_app_be.model.enums.AuditActionType;
import io.propenuy.asis_app_be.model.enums.AuditModuleCode;
import io.propenuy.asis_app_be.model.enums.IncomeCategory;
import io.propenuy.asis_app_be.model.enums.PaymentMethod;
import io.propenuy.asis_app_be.model.enums.SourceType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class IncomeTransactionAuditRecorder {

    private final AuditLogWriter auditLogWriter;
    private final IncomeTransactionAuditSnapshot snapshot;

    public IncomeTransactionAuditRecorder(
            AuditLogWriter auditLogWriter,
            IncomeTransactionAuditSnapshot snapshot) {
        this.auditLogWriter = auditLogWriter;
        this.snapshot = snapshot;
    }

    public record BeforeState(
            LocalDate transactionDate,
            IncomeCategory category,
            SourceType sourceType,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            String donorName,
            String note,
            String proofFilePath,
            String status,
            LocalDateTime deletedAt) {
    }

    public BeforeState capture(IncomeTransaction transaction) {
        return new BeforeState(
                transaction.getTransactionDate(),
                transaction.getCategory(),
                transaction.getSourceType(),
                transaction.getPaymentMethod(),
                transaction.getAmount(),
                transaction.getDonorName(),
                transaction.getNote(),
                transaction.getProofFilePath(),
                transaction.getStatus(),
                transaction.getDeletedAt());
    }

    public void recordAfterCreate(IncomeTransaction transaction) {
        auditLogWriter.persist(
                AuditModuleCode.INCOME_TRANSACTION,
                IncomeTransaction.class,
                transaction.getId(),
                AuditActionType.CREATE,
                null,
                snapshot.toJson(transaction, null));
    }

    public void recordAfterUpdate(BeforeState before, IncomeTransaction after, boolean proofChanged) {
        auditLogWriter.persist(
                AuditModuleCode.INCOME_TRANSACTION,
                IncomeTransaction.class,
                after.getId(),
                AuditActionType.UPDATE,
                snapshot.toJson(before, null),
                snapshot.toJson(after, proofChanged));
    }

    public void recordAfterSoftDelete(BeforeState before, UUID transactionId) {
        auditLogWriter.persist(
                AuditModuleCode.INCOME_TRANSACTION,
                IncomeTransaction.class,
                transactionId,
                AuditActionType.DELETE,
                snapshot.toJson(before, null),
                null);
    }
}
