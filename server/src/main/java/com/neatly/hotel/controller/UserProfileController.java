package com.neatly.hotel.controller;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.neatly.hotel.dto.ApiResponse;
import com.neatly.hotel.service.ProfilePictureService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/profiles")
@Tag(name = "User Profiles")
public class UserProfileController {

	private final ProfilePictureService profilePictureService;

	public UserProfileController(ProfilePictureService profilePictureService) {
		this.profilePictureService = profilePictureService;
	}

	@PostMapping(path = "/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(
			summary = "Upload the signed-in user's profile picture",
			description = "Requires a Clerk session token. Multipart field `file`: PNG, JPEG or WEBP, max 5 MB.")
	@SecurityRequirement(name = "clerkBearer")
	public ApiResponse<String> uploadPicture(
			@AuthenticationPrincipal Jwt jwt,
			@RequestPart("file") MultipartFile file) {
		return ApiResponse.ok("Profile picture uploaded", profilePictureService.upload(jwt.getSubject(), file));
	}
}
