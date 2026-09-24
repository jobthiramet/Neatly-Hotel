package com.neatly.hotel.dto;

import java.math.BigDecimal;

public record PromotionCodePreviewResponse(
		PromotionPreviewStatus status,
		BigDecimal discountAmount,
		BigDecimal minPurchaseAmount) {

	public static PromotionCodePreviewResponse notFound() {
		return new PromotionCodePreviewResponse(PromotionPreviewStatus.NOT_FOUND, null, null);
	}
}
