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
            SELECT COALESCE(SUM(i.amount), 0)
            FROM IncomeTransaction i
            WHERE i.status = 'CONFIRMED'
              AND i.deletedAt IS NULL
              AND i.transactionDate >= :startDate
              AND i.transactionDate <= :endDate
            """)
    BigDecimal sumConfirmedBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

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

    /**
     * Aggregasi donasi tunai (kategori DONASI) per bulan — native SQL untuk PostgreSQL
     * (selaras dengan {@code spring.jpa.properties.hibernate.dialect=PostgreSQLDialect}).
     */
    @Query(value = """
            SELECT CAST(EXTRACT(YEAR FROM i.transaction_date) AS INTEGER),
                   CAST(EXTRACT(MONTH FROM i.transaction_date) AS INTEGER),
                   COALESCE(SUM(i.amount), 0)
            FROM income_transactions i
            WHERE i.status = 'CONFIRMED'
              AND i.deleted_at IS NULL
              AND i.category = 'DONASI'
              AND i.transaction_date >= :startDate
              AND i.transaction_date <= :endDate
            GROUP BY EXTRACT(YEAR FROM i.transaction_date), EXTRACT(MONTH FROM i.transaction_date)
            ORDER BY EXTRACT(YEAR FROM i.transaction_date), EXTRACT(MONTH FROM i.transaction_date)
            """, nativeQuery = true)
    List<Object[]> sumDonationByYearMonthBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(value = """
            SELECT CAST(EXTRACT(YEAR FROM i.transaction_date) AS INTEGER),
                   CAST(EXTRACT(QUARTER FROM i.transaction_date) AS INTEGER),
                   COALESCE(SUM(i.amount), 0)
            FROM income_transactions i
            WHERE i.status = 'CONFIRMED'
              AND i.deleted_at IS NULL
              AND i.category = 'DONASI'
              AND i.transaction_date >= :startDate
              AND i.transaction_date <= :endDate
            GROUP BY EXTRACT(YEAR FROM i.transaction_date), EXTRACT(QUARTER FROM i.transaction_date)
            ORDER BY EXTRACT(YEAR FROM i.transaction_date), EXTRACT(QUARTER FROM i.transaction_date)
            """, nativeQuery = true)
    List<Object[]> sumDonationByYearQuarterBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query(value = """
            SELECT CAST(EXTRACT(YEAR FROM i.transaction_date) AS INTEGER),
                   COALESCE(SUM(i.amount), 0)
            FROM income_transactions i
            WHERE i.status = 'CONFIRMED'
              AND i.deleted_at IS NULL
              AND i.category = 'DONASI'
              AND i.transaction_date >= :startDate
              AND i.transaction_date <= :endDate
            GROUP BY EXTRACT(YEAR FROM i.transaction_date)
            ORDER BY EXTRACT(YEAR FROM i.transaction_date)
            """, nativeQuery = true)
    List<Object[]> sumDonationByYearBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
