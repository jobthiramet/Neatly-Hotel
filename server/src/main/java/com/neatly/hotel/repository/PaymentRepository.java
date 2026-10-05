package com.neatly.hotel.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.hotel.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

	Optional<Payment> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);
}
