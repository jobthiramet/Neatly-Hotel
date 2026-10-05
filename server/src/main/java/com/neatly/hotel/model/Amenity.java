package com.neatly.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "amenities")
public class Amenity extends BaseEntity {

	/** Unique, case-insensitive. */
	@Column(nullable = false, unique = true, length = 120)
	private String name;
}
