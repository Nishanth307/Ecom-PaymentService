package com.PaymentService.paymentservice.repositories;

import com.PaymentService.paymentservice.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByPaymentLinkId(String paymentLinkId);
    Optional<Payment> findByPaymentId(String paymentId);
}

