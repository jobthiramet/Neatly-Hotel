package com.neatly.hotel.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.neatly.hotel.model.Profile;

public record ProfileResponse(
		String clerkUserId,
		String firstName,
		String lastName,
		String phoneNumber,
		LocalDate dateOfBirth,
		String country,
		String profilePicture,
		String role,
		Instant createdAt,
		Instant updatedAt) {

	public static ProfileResponse from(Profile profile) {
		return new ProfileResponse(
				profile.getClerkUserId(),
				profile.getFirstName(),
				profile.getLastName(),
				profile.getPhoneNumber(),
				profile.getDateOfBirth(),
				profile.getCountry(),
				profile.getProfilePicture(),
				profile.getRole(),
				profile.getCreatedAt(),
				profile.getUpdatedAt());
	}
}
