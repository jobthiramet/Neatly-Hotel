package com.neatly.hotel.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.hotel.dto.AnalyticsResponse;
import com.neatly.hotel.dto.ApiResponse;
import com.neatly.hotel.service.AnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/analytics")
@Tag(name = "Admin Analytics")
public class AnalyticsController {

	private final AnalyticsService analyticsService;

	public AnalyticsController(AnalyticsService analyticsService) {
		this.analyticsService = analyticsService;
	}

	@GetMapping
	@SecurityRequirement(name = "clerkBearer")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid Clerk token")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Profile is missing or role is not agent")
	@Operation(summary = "Get dashboard analytics", description = "Returns booking analytics for an inclusive date range. Requires agent role.")
	public ApiResponse<AnalyticsResponse> get(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return ApiResponse.ok(analyticsService.get(from, to));
	}
}
