package io.propenuy.asis_app_be.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.propenuy.asis_app_be.model.PaymentRequestBreakdown;

public interface PaymentRequestBreakdownRepository extends JpaRepository<PaymentRequestBreakdown, UUID> {
}
