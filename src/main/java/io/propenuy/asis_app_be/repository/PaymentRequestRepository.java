package io.propenuy.asis_app_be.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.propenuy.asis_app_be.model.PaymentRequest;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, UUID>,
        JpaSpecificationExecutor<PaymentRequest> {

    @Query("SELECT DISTINCT p FROM PaymentRequest p "
            + "LEFT JOIN FETCH p.breakdowns "
            + "LEFT JOIN FETCH p.createdBy "
            + "LEFT JOIN FETCH p.updatedBy "
            + "WHERE p.id = :id")
    Optional<PaymentRequest> findDetailById(@Param("id") UUID id);
}
