package com.neatly.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.hotel.model.StripeWebhookEvent;

public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, String> {
}
