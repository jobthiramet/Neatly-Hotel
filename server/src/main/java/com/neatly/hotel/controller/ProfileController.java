package com.neatly.hotel.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.hotel.dto.ApiResponse;
import com.neatly.hotel.dto.CreateProfileRequest;
import com.neatly.hotel.dto.ProfileResponse;
import com.neatly.hotel.dto.ProfileUpdateResponse;
import com.neatly.hotel.dto.UpdateProfileRequest;
import com.neatly.hotel.service.ProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profiles")
@Tag(name = "User Profiles")
@SecurityRequirement(name = "clerkBearer")
public class ProfileController {

	private final ProfileService profileService;

	public ProfileController(ProfileService profileService) {
		this.profileService = profileService;
	}

	@GetMapping("/me")
	@Operation(
			summary = "Get the signed-in user's profile",
			description = "The user comes from the verified Clerk token. Guests without a saved profile "
					+ "get an empty one to fill in; nothing is written until they save.")
	public ApiResponse<ProfileResponse> getCurrent(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.ok(profileService.findOrDefault(jwt.getSubject()));
	}

	@PutMapping("/me")
	@Operation(
			summary = "Update the signed-in user's profile",
			description = "Creates the profile on first save. The user comes from the verified Clerk token; "
					+ "any id in the body is ignored. Phone numbers are stored in E.164. The name is mirrored "
					+ "to Clerk afterwards: `clerkSynced` is false when that call failed (the save still stands) "
					+ "and null when Clerk mirroring is not configured. Email and password are changed in Clerk.")
	public ApiResponse<ProfileUpdateResponse> updateCurrent(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody UpdateProfileRequest request) {
		return ApiResponse.ok("Profile updated", profileService.update(jwt.getSubject(), request));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a user profile")
	public ApiResponse<ProfileResponse> create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateProfileRequest request) {
		return ApiResponse.ok("Profile created", profileService.create(jwt.getSubject(), request));
	}
}
