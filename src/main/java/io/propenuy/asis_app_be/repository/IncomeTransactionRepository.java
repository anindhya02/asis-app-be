package io.propenuy.asis_app_be.repository;

import io.propenuy.asis_app_be.model.IncomeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.UUID;

public interface IncomeTransactionRepository extends JpaRepository<IncomeTransaction, UUID>,
        JpaSpecificationExecutor<IncomeTransaction> {

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM IncomeTransaction i WHERE i.status = 'CONFIRMED'")
    BigDecimal sumAllConfirmedIncome();
}
