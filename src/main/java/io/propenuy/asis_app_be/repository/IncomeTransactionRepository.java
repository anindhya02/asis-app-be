package io.propenuy.asis_app_be.repository;

import io.propenuy.asis_app_be.model.IncomeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface IncomeTransactionRepository extends JpaRepository<IncomeTransaction, UUID>,
        JpaSpecificationExecutor<IncomeTransaction> {
}
