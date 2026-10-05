package com.neatly.hotel.dto;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.neatly.hotel.model.DiscountType;
import com.neatly.hotel.model.PromotionCode;
import com.neatly.hotel.model.RoomType;

public record PromotionCodeResponse(
		UUID id,
		String code,
		DiscountType discountType,
		BigDecimal amountOff,
		BigDecimal percentOff,
		BigDecimal minPurchaseAmount,
		List<RoomTypeRef> roomTypes) {

	/** Empty {@code roomTypes} means the code applies to every room type. */
	public record RoomTypeRef(UUID id, String name) {
	}

	public static PromotionCodeResponse from(PromotionCode promo) {
		List<RoomTypeRef> types = promo.getRoomTypes().stream()
				.filter(type -> type.getDeletedAt() == null)
				.sorted(Comparator.comparing(RoomType::getName, String.CASE_INSENSITIVE_ORDER))
				.map(type -> new RoomTypeRef(type.getId(), type.getName()))
				.toList();
		return new PromotionCodeResponse(
				promo.getId(),
				promo.getCode(),
				promo.getDiscountType(),
				promo.getAmountOff(),
				promo.getPercentOff(),
				promo.getMinPurchaseAmount(),
				types);
	}
}
