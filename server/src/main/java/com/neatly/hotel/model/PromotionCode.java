package com.neatly.hotel.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "promotion_codes")
public class PromotionCode extends BaseEntity {

	@Column(nullable = false, length = 40)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private DiscountType discountType = DiscountType.FIXED;

	/** Set when {@link #discountType} is {@link DiscountType#FIXED}. */
	@Column(precision = 12, scale = 2)
	private BigDecimal amountOff;

	/** Set when {@link #discountType} is {@link DiscountType#PERCENT}. Greater than 0 and at most 100. */
	@Column(precision = 5, scale = 2)
	private BigDecimal percentOff;

	/** Pre-discount total (room + extras) required before the code applies. */
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal minPurchaseAmount = BigDecimal.ZERO;

	@Column(nullable = false)
	private Boolean active = true;

	/** Soft delete. Checkout and the admin list skip these so past bookings can keep the row. */
	private Instant deletedAt;

	/** Empty means every room type. */
	@ManyToMany
	@JoinTable(
			name = "promotion_code_room_types",
			joinColumns = @JoinColumn(name = "promotion_code_id"),
			inverseJoinColumns = @JoinColumn(name = "room_type_id"))
	private Set<RoomType> roomTypes = new LinkedHashSet<>();
}
