package io.propenuy.asis_app_be.audit;

import io.propenuy.asis_app_be.model.ExpenseTransaction;
import io.propenuy.asis_app_be.model.enums.AuditActionType;
import io.propenuy.asis_app_be.model.enums.AuditModuleCode;
import io.propenuy.asis_app_be.model.enums.ExpenseCategory;
import io.propenuy.asis_app_be.model.enums.PaymentMethod;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ExpenseTransactionAuditRecorder {

    private final AuditLogWriter auditLogWriter;
    private final ExpenseTransactionAuditSnapshot snapshot;

    public ExpenseTransactionAuditRecorder(
            AuditLogWriter auditLogWriter,
            ExpenseTransactionAuditSnapshot snapshot) {
        this.auditLogWriter = auditLogWriter;
        this.snapshot = snapshot;
    }

    public record BeforeState(
            LocalDate transactionDate,
            ExpenseCategory category,
            String subCategory,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            String note,
            String proofFilePath,
            String status,
            LocalDateTime deletedAt) {
    }

    public BeforeState capture(ExpenseTransaction transaction) {
        return new BeforeState(
                transaction.getTransactionDate(),
                transaction.getCategory(),
                transaction.getSubCategory(),
                transaction.getPaymentMethod(),
                transaction.getAmount(),
                transaction.getNote(),
                transaction.getProofFilePath(),
                transaction.getStatus(),
                transaction.getDeletedAt());
    }

    public void recordAfterCreate(ExpenseTransaction transaction) {
        auditLogWriter.persist(
                AuditModuleCode.EXPENSE_TRANSACTION,
                ExpenseTransaction.class,
                transaction.getId(),
                AuditActionType.CREATE,
                null,
                snapshot.toJson(transaction, null));
    }

    public void recordAfterUpdate(BeforeState before, ExpenseTransaction after, boolean proofChanged) {
        auditLogWriter.persist(
                AuditModuleCode.EXPENSE_TRANSACTION,
                ExpenseTransaction.class,
                after.getId(),
                AuditActionType.UPDATE,
                snapshot.toJson(before, null),
                snapshot.toJson(after, proofChanged));
    }

    public void recordAfterSoftDelete(BeforeState before, UUID transactionId) {
        auditLogWriter.persist(
                AuditModuleCode.EXPENSE_TRANSACTION,
                ExpenseTransaction.class,
                transactionId,
                AuditActionType.DELETE,
                snapshot.toJson(before, null),
                null);
    }
}
