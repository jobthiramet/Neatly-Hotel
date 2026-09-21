package com.neatly.hotel.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Query params of GET /api/rooms/available. Caps match CreateBookingRequest. */
public record RoomAvailabilityQuery(
		@NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
		@NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
		@NotNull @Min(1) @Max(10) Integer rooms,
		@NotNull @Min(1) @Max(6) Integer guests,
		/** Optional filter; empty means every room type. Unknown ids simply match nothing. */
		@Size(max = 20) List<UUID> roomTypeIds) {

	public List<UUID> types() {
		return roomTypeIds == null ? List.of() : roomTypeIds;
	}

	@AssertTrue(message = "check-out must be after check-in")
	public boolean isCheckOutValid() {
		return checkIn == null || checkOut == null || checkOut.isAfter(checkIn);
	}
}
