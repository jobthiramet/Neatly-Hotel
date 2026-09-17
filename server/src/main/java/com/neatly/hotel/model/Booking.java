package com.neatly.hotel.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

	@Column(nullable = false, length = 64)
	private String clerkUserId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_type_id", nullable = false)
	private Room room;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "promotion_code_id")
	private PromotionCode promotionCode;

	@Column(nullable = false)
	private LocalDate checkIn;

	@Column(nullable = false)
	private LocalDate checkOut;

	@Column(nullable = false)
	private Integer guests;

	@Column(nullable = false)
	private Integer roomsCount = 1;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private BookingStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BookingPaymentMethod paymentMethod;

	@Column(nullable = false, length = 100)
	private String guestFirstName;

	@Column(nullable = false, length = 100)
	private String guestLastName;

	@Column(nullable = false, length = 254)
	private String guestEmail;

	@Column(nullable = false, length = 32)
	private String guestPhone;

	@Column(nullable = false, length = 100)
	private String guestCountry;

	@Column(nullable = false)
	private LocalDate guestDateOfBirth;

	@Column(nullable = false, columnDefinition = "text")
	private String standardRequests = "[]";

	@Column(columnDefinition = "text")
	private String additionalRequest;

	@Column(nullable = false, length = 3)
	private String currency = "THB";

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal roomSubtotal;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal extrasTotal;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal discountTotal;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal grandTotal;

	@Column(nullable = false, length = 120)
	private String roomNameSnapshot;

	@Column(columnDefinition = "text")
	private String roomImageUrl;

	private Instant holdExpiresAt;

	private Instant cancelledAt;

	private Instant checkedInAt;

	@OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sortOrder ASC")
	private List<BookingItem> items = new ArrayList<>();

	@OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("createdAt ASC")
	private List<Payment> payments = new ArrayList<>();
}
