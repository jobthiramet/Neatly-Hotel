package com.neatly.hotel.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stripe_events")
public class StripeWebhookEvent {

	@Id
	@Column(name = "event_id", nullable = false, length = 255)
	private String eventId;

	@Column(nullable = false, length = 120)
	private String type;

	@Column(length = 255)
	private String stripeObjectId;

	@Column(nullable = false)
	private Instant processedAt;
}
