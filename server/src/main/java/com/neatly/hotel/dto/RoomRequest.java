package com.neatly.hotel.dto;

import java.math.BigDecimal;
import java.util.List;

import com.neatly.hotel.model.BedType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Room fields for create and update (Figma: admin room & property form). */
public record RoomRequest(
		@Schema(description = "Room type, e.g. Superior Garden View. Unique among non-deleted rooms (case-insensitive).")
		@NotBlank @Size(max = 120) String name,
		@NotNull BedType bedType,
		@NotNull @Min(1) @Max(10000) Integer sizeSqm,
		@Schema(description = "Guests")
		@NotNull @Min(2) @Max(6) Integer capacity,
		@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal pricePerNight,
		@Schema(description = "Optional; must be lower than pricePerNight")
		@DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal promotionPrice,
		@NotBlank @Size(max = 5000) String description,
		@NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 120) String> amenities) {

	@Schema(hidden = true)
	@AssertTrue(message = "must be lower than pricePerNight")
	public boolean isPromotionPriceValid() {
		return promotionPrice == null || pricePerNight == null || promotionPrice.compareTo(pricePerNight) < 0;
	}
}
