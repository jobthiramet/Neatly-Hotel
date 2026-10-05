package com.neatly.hotel.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record ChangeBookingDatesRequest(
		@NotNull LocalDate checkIn,
		@NotNull LocalDate checkOut) {

	@AssertTrue(message = "check-out must be after check-in")
	public boolean isStayValid() {
		return checkIn == null || checkOut == null || checkOut.isAfter(checkIn);
	}
}
