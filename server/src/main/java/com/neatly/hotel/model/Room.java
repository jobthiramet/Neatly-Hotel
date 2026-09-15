package com.neatly.hotel.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(name = "uk_rooms_room_number", columnNames = "room_number"))
public class Room extends BaseEntity {

	/** Allowed housekeeping / occupancy labels (aligned with client Badge statuses). */
	public static final Set<String> ALLOWED_STATUSES = new LinkedHashSet<>(Arrays.asList(
			"Vacant",
			"Occupied",
			"Assign Clean",
			"Assign Dirty",
			"Vacant Clean",
			"Vacant Clean Inspected",
			"Vacant Clean Pick Up",
			"Occupied Clean",
			"Occupied Clean Inspected",
			"Occupied Dirty",
			"Out of Order",
			"Out of Service",
			"Out of Inventory"));

	@Column(name = "room_number", nullable = false, length = 20)
	private String roomNumber;

	@Column(name = "room_type", nullable = false, length = 120)
	private String roomType;

	@Column(name = "bed_type", nullable = false, length = 50)
	private String bedType;

	@Column(nullable = false, length = 50)
	private String status = "Vacant Clean";

	@Column(name = "price_per_night", nullable = false, precision = 12, scale = 2)
	private BigDecimal pricePerNight = BigDecimal.ZERO;

	@Column(nullable = false)
	private Integer capacity = 2;

	@Column(nullable = false)
	private Boolean active = true;
}
