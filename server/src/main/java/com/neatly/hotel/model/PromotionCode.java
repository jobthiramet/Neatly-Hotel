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
@Table(name = "promotion_codes")
public class PromotionCode extends BaseEntity {

	@Column(nullable = false, length = 40)
	private String code;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amountOff;

	@Column(nullable = false)
	private Boolean active = true;
}
