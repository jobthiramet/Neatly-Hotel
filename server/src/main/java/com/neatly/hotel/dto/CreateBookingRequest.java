package com.neatly.hotel.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.neatly.hotel.model.BookingPaymentMethod;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(
		@NotNull UUID roomTypeId,
		@NotNull LocalDate checkIn,
		@NotNull LocalDate checkOut,
		@NotNull @Min(1) @Max(6) Integer guests,
		@Min(1) @Max(10) Integer roomsCount,
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(max = 32) String phoneNumber,
		@NotBlank @Size(max = 100) String country,
		@NotNull @Past LocalDate dateOfBirth,
		List<@NotBlank @Size(max = 80) String> standardRequestCodes,
		List<@NotBlank @Size(max = 80) String> specialRequestCodes,
		@Size(max = 2000) String additionalRequest,
		@Size(max = 40) String promotionCode,
		@NotNull BookingPaymentMethod paymentMethod) {

	@AssertTrue(message = "check-out must be after check-in")
	public boolean isStayValid() {
		return checkIn == null || checkOut == null || checkOut.isAfter(checkIn);
	}

	@AssertTrue(message = "must be at least 18 years old")
	public boolean isAtLeast18YearsOld() {
		return dateOfBirth == null || !dateOfBirth.isAfter(LocalDate.now().minusYears(18));
	}

	public int rooms() {
		return roomsCount == null ? 1 : roomsCount;
	}
}
