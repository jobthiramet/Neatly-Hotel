package com.neatly.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hotel_info")
public class HotelInfo extends BaseEntity {

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, columnDefinition = "text")
	private String description;

	@Column(length = 500)
	private String logoUrl;
}
