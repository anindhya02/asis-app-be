package io.propenuy.asis_app_be.repository;

import io.propenuy.asis_app_be.model.PaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, UUID>,
        JpaSpecificationExecutor<PaymentRequest> {
}
