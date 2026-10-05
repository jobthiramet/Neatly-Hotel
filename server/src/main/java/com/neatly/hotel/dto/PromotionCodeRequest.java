package com.neatly.hotel.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.neatly.hotel.model.DiscountType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PromotionCodeRequest(
		@NotBlank
		@Size(max = 40)
		@Pattern(regexp = "^[A-Za-z0-9-]{1,40}$", message = "must contain only letters, numbers, and hyphens")
		String code,
		@NotNull DiscountType discountType,
		@DecimalMin(value = "0.01")
		@Digits(integer = 10, fraction = 2)
		BigDecimal amountOff,
		@DecimalMin(value = "0.01")
		@DecimalMax(value = "100")
		@Digits(integer = 3, fraction = 2)
		BigDecimal percentOff,
		@NotNull
		@DecimalMin(value = "0.00")
		@Digits(integer = 10, fraction = 2)
		BigDecimal minPurchaseAmount,
		@Size(max = 50) List<UUID> roomTypeIds) {
}
