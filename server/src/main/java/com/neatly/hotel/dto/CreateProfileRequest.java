package com.neatly.hotel.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

public record CreateProfileRequest(
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@NotBlank
		@Pattern(regexp = "\\+[1-9][0-9]{7,14}", message = "must be in E.164 format")
		String phoneNumber,
		@NotNull @Past LocalDate dateOfBirth,
		@NotBlank @Size(max = 100) String country,
		@Size(max = 500) String profilePicture) {

	@AssertTrue(message = "must be at least 18 years old")
	public boolean isAtLeast18YearsOld() {
		return dateOfBirth != null && !dateOfBirth.isAfter(LocalDate.now().minusYears(18));
	}
}
