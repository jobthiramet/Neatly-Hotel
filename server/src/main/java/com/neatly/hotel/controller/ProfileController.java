package com.neatly.hotel.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.hotel.dto.ApiResponse;
import com.neatly.hotel.dto.CreateProfileRequest;
import com.neatly.hotel.dto.ProfileResponse;
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
	@Operation(summary = "Get the signed-in user's profile")
	public ApiResponse<ProfileResponse> getCurrent(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.ok(profileService.findByClerkUserId(jwt.getSubject()));
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
