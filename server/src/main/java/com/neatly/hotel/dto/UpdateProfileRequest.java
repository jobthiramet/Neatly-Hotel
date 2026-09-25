package com.neatly.hotel.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Profile fields the guest can edit. Email and password stay with Clerk. */
public record UpdateProfileRequest(
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		/** Local ("088 888 8888") or E.164; stored as E.164. */
		@NotBlank
		@Pattern(regexp = "\\+?[0-9][0-9 -]{7,19}", message = "must be a valid phone number")
		String phoneNumber,
		@NotNull @Past LocalDate dateOfBirth,
		@NotBlank @Size(max = 100) String country,
		@Size(max = 500) String profilePicture) {

	@AssertTrue(message = "must be at least 18 years old")
	public boolean isAtLeast18YearsOld() {
		return dateOfBirth != null && !dateOfBirth.isAfter(LocalDate.now().minusYears(18));
	}
}
