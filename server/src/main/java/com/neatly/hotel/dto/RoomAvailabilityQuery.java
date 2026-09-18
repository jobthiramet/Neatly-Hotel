package com.neatly.hotel.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Query params of GET /api/rooms/available. Caps match CreateBookingRequest. */
public record RoomAvailabilityQuery(
		@NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
		@NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
		@NotNull @Min(1) @Max(10) Integer rooms,
		@NotNull @Min(1) @Max(6) Integer guests) {

	@AssertTrue(message = "check-out must be after check-in")
	public boolean isCheckOutValid() {
		return checkIn == null || checkOut == null || checkOut.isAfter(checkIn);
	}
}
