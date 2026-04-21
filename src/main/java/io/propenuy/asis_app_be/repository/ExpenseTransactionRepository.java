package io.propenuy.asis_app_be.repository;

import io.propenuy.asis_app_be.model.ExpenseTransaction;
import io.propenuy.asis_app_be.model.enums.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseTransactionRepository extends JpaRepository<ExpenseTransaction, UUID>,
        JpaSpecificationExecutor<ExpenseTransaction> {

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ExpenseTransaction e WHERE e.status = 'ACTIVE'")
    BigDecimal sumAllActiveExpenses();

    @Query("""
            SELECT e.category, COALESCE(SUM(e.amount), 0)
            FROM ExpenseTransaction e
            WHERE e.status = 'ACTIVE'
              AND e.deletedAt IS NULL
              AND e.transactionDate >= :startDate
              AND e.transactionDate <= :endDate
            GROUP BY e.category
            """)
    List<Object[]> sumAmountByCategoryBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM ExpenseTransaction e
            WHERE e.status = 'ACTIVE'
              AND e.deletedAt IS NULL
              AND e.transactionDate >= :startDate
              AND e.transactionDate <= :endDate
              AND e.category = :category
            """)
    BigDecimal sumAmountBetweenForCategory(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") ExpenseCategory category);
}
