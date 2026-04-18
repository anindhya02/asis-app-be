package io.propenuy.asis_app_be.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import io.propenuy.asis_app_be.model.PaymentRequestReviewActivity;

public interface PaymentRequestReviewActivityRepository extends JpaRepository<PaymentRequestReviewActivity, UUID> {

    @EntityGraph(attributePaths = { "actor" })
    List<PaymentRequestReviewActivity> findByPaymentRequest_IdOrderByCreatedAtAsc(UUID paymentRequestId);
}
