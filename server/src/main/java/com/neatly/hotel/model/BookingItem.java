package com.neatly.hotel.model;

import java.math.BigDecimal;

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
@Table(name = "booking_items")
public class BookingItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "booking_id", nullable = false)
	private Booking booking;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BookingItemKind kind;

	@Column(nullable = false, length = 80)
	private String code;

	@Column(nullable = false, length = 200)
	private String label;

	@Column(nullable = false)
	private Integer quantity = 1;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false)
	private Integer sortOrder = 0;
}
