package com.neatly.hotel.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "room_types")
public class RoomType extends BaseEntity {

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

	/** How many physical rooms of this type can be sold on overlapping dates. */
	@Column(nullable = false, columnDefinition = "integer not null default 4")
	private Integer totalUnits = 4;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private BedType bedType;

	@Column(nullable = false)
	private Integer sizeSqm;

	@Column(nullable = false, columnDefinition = "text")
	private String description;

	/** Display order is list order. */
	@ManyToMany
	@JoinTable(
			name = "room_type_amenities",
			joinColumns = @JoinColumn(name = "room_type_id"),
			inverseJoinColumns = @JoinColumn(name = "amenity_id"))
	@OrderColumn(name = "sort_order")
	private List<Amenity> amenities = new ArrayList<>();

	/** Soft delete. Queries filter on this explicitly so relations can still load deleted rooms. */
	private Instant deletedAt;

	@OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL)
	@OrderBy("sortOrder ASC")
	private List<RoomTypeImage> images = new ArrayList<>();
}
