package com.neatly.hotel.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "booking_id", nullable = false)
	private Booking booking;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_payment_id")
	private Payment parentPayment;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentProvider provider;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentKind kind;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentStatus status;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency = "THB";

	@Column(length = 255)
	private String stripeCheckoutSessionId;

	@Column(length = 255)
	private String stripePaymentIntentId;

	@Column(length = 255)
	private String stripeRefundId;

	@Column(length = 40)
	private String cardBrand;

	@Column(length = 4)
	private String cardLast4;

	private Instant paidAt;

	@Column(columnDefinition = "text")
	private String failureMessage;
}
