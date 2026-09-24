package com.neatly.hotel.service;

import java.math.BigDecimal;

import com.neatly.hotel.model.Booking;

public interface StripeCheckoutGateway {

	record SessionResult(
			String checkoutSessionId,
			String clientSecret,
			String paymentIntentId) {
	}

	record SessionView(
			String checkoutSessionId,
			String clientSecret,
			String status,
			String paymentStatus,
			String paymentIntentId,
			String cardBrand,
			String cardLast4) {
	}

	record WebhookEvent(
			String eventId,
			String type,
			String checkoutSessionId) {
	}

	SessionResult createSession(Booking booking, String returnUrl);

	/** Replaces the open session's charge with {@code booking}'s current total. The client secret stays the same. */
	void updateSessionAmount(String checkoutSessionId, Booking booking);

	SessionView retrieveSession(String checkoutSessionId);

	WebhookEvent parseEvent(String payload, String signature);

	String refund(String paymentIntentId, BigDecimal amount);
}
