package com.neatly.hotel.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.neatly.hotel.dto.ApiResponse;
import com.neatly.hotel.dto.HotelInfoResponse;
import com.neatly.hotel.dto.UpdateHotelInfoRequest;
import com.neatly.hotel.service.HotelInfoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/hotel")
@Tag(name = "Hotel Information")
public class HotelInfoController {

	private final HotelInfoService hotelInfoService;

	public HotelInfoController(HotelInfoService hotelInfoService) {
		this.hotelInfoService = hotelInfoService;
	}

	@GetMapping
	@Operation(summary = "Get hotel information", description = "Public. Hotel name, description and logo URL.")
	public ApiResponse<HotelInfoResponse> get() {
		return ApiResponse.ok(hotelInfoService.get());
	}

	// TODO(auth): restrict to admins once Supabase JWT auth is wired.
	@PutMapping
	@Operation(summary = "Update hotel name and description")
	public ApiResponse<HotelInfoResponse> update(@Valid @RequestBody UpdateHotelInfoRequest request) {
		return ApiResponse.ok("Hotel information updated", hotelInfoService.update(request));
	}

	// TODO(auth): restrict to admins once Supabase JWT auth is wired.
	@PutMapping(path = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(
			summary = "Upload and replace hotel logo",
			description = "Multipart field `file`: PNG, JPEG or WEBP, max 2 MB. Returns 503 when storage is not configured.")
	public ApiResponse<HotelInfoResponse> replaceLogo(@RequestPart("file") MultipartFile file) {
		return ApiResponse.ok("Hotel logo updated", hotelInfoService.replaceLogo(file));
	}
}
