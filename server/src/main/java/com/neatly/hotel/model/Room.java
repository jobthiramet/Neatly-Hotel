package com.neatly.hotel.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "room_types")
public class Room extends BaseEntity {

	/** Shown as "Room type" in the admin UI. */
	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal pricePerNight;

	@Column(precision = 12, scale = 2)
	private BigDecimal promotionPrice;

	/** Guests. */
	@Column(nullable = false)
	private Integer capacity = 2;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private BedType bedType;

	@Column(nullable = false)
	private Integer sizeSqm;

	@Column(nullable = false, columnDefinition = "text")
	private String description;

	/** Display order is array order. */
	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(nullable = false)
	private List<String> amenities = new ArrayList<>();

	/** Soft delete. Queries filter on this explicitly so relations can still load deleted rooms. */
	private Instant deletedAt;

	@OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
	@OrderBy("sortOrder ASC")
	private List<RoomImage> images = new ArrayList<>();
}
