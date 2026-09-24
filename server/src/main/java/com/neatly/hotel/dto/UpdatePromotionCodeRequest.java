package com.neatly.hotel.dto;

import jakarta.validation.constraints.Size;

/** Blank or omitted clears the code. Unknown codes are ignored at pricing time. */
public record UpdatePromotionCodeRequest(
		@Size(max = 40) String promotionCode) {
}
