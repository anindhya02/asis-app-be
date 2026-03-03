package io.propenuy.asis_app_be.repository;

import io.propenuy.asis_app_be.model.ExpenseTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.UUID;

public interface ExpenseTransactionRepository extends JpaRepository<ExpenseTransaction, UUID>,
        JpaSpecificationExecutor<ExpenseTransaction> {

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ExpenseTransaction e WHERE e.status = 'ACTIVE'")
    BigDecimal sumAllActiveExpenses();
}
