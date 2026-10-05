package com.neatly.hotel.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.hotel.dto.AdminBookingDetailResponse;
import com.neatly.hotel.dto.AdminBookingSummaryResponse;
import com.neatly.hotel.dto.ApiResponse;
import com.neatly.hotel.dto.PageResponse;
import com.neatly.hotel.service.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/bookings")
@Tag(name = "Admin bookings")
public class AdminBookingController {

	private final BookingService bookingService;

	public AdminBookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@GetMapping
	@SecurityRequirement(name = "clerkBearer")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid Clerk token")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Profile is missing or role is not agent")
	@Operation(
			summary = "List customer bookings",
			description = "Admin Customer Booking table. Confirmed and later statuses, newest first. Optional search on guest name, room type, or booking number.")
	public ApiResponse<PageResponse<AdminBookingSummaryResponse>> list(
			@RequestParam(required = false, defaultValue = "") String search,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ApiResponse.ok(bookingService.listForAdmin(search, PageRequest.of(page, size)));
	}

	@GetMapping("/{id}")
	@SecurityRequirement(name = "clerkBearer")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid Clerk token")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Profile is missing or role is not agent")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found")
	@Operation(summary = "Get customer booking detail", description = "Admin Customer Booking detail page.")
	public ApiResponse<AdminBookingDetailResponse> get(@PathVariable UUID id) {
		return ApiResponse.ok(bookingService.findForAdmin(id));
	}
}
