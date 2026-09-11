package com.neatly.hotel.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rooms")
public class Room extends BaseEntity {

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, length = 50)
	private String type;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal pricePerNight;

	@Column(nullable = false)
	private Integer capacity = 2;

	@Column(nullable = false)
	private Boolean active = true;
}
