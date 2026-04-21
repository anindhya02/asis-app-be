package io.propenuy.asis_app_be.repository;

import io.propenuy.asis_app_be.model.IncomeTransaction;
import io.propenuy.asis_app_be.model.enums.IncomeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IncomeTransactionRepository extends JpaRepository<IncomeTransaction, UUID>,
        JpaSpecificationExecutor<IncomeTransaction> {

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM IncomeTransaction i WHERE i.status = 'CONFIRMED'")
    BigDecimal sumAllConfirmedIncome();

    @Query("""
            SELECT i.category, COALESCE(SUM(i.amount), 0)
            FROM IncomeTransaction i
            WHERE i.status = 'CONFIRMED'
              AND i.deletedAt IS NULL
              AND i.transactionDate >= :startDate
              AND i.transactionDate <= :endDate
            GROUP BY i.category
            """)
    List<Object[]> sumAmountByCategoryBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(i.amount), 0)
            FROM IncomeTransaction i
            WHERE i.status = 'CONFIRMED'
              AND i.deletedAt IS NULL
              AND i.transactionDate >= :startDate
              AND i.transactionDate <= :endDate
              AND i.category = :category
            """)
    BigDecimal sumAmountBetweenForCategory(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") IncomeCategory category);
}
