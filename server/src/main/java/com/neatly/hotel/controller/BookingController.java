package com.neatly.hotel.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.hotel.dto.ApiResponse;
import com.neatly.hotel.dto.BookingResponse;
import com.neatly.hotel.dto.CreateBookingRequest;
import com.neatly.hotel.dto.PageResponse;
import com.neatly.hotel.service.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings")
@SecurityRequirement(name = "clerkBearer")
public class BookingController {

	private final BookingService bookingService;

	public BookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a booking", description = "Cash confirms immediately. Card returns a Stripe Checkout client secret.")
	public ApiResponse<BookingResponse> create(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateBookingRequest request) {
		return ApiResponse.ok("Booking created", bookingService.create(jwt.getSubject(), request));
	}

	@GetMapping
	@Operation(summary = "List the signed-in user's bookings")
	public ApiResponse<PageResponse<BookingResponse>> list(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ApiResponse.ok(bookingService.listMine(jwt.getSubject(), PageRequest.of(page, size)));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a booking owned by the signed-in user")
	public ApiResponse<BookingResponse> get(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id) {
		return ApiResponse.ok(bookingService.findMine(jwt.getSubject(), id));
	}

	@PostMapping("/{id}/payment-session")
	@Operation(summary = "Create a new Stripe Checkout session for an unpaid card booking")
	public ApiResponse<BookingResponse> retryPayment(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID id) {
		return ApiResponse.ok(bookingService.retryPayment(jwt.getSubject(), id));
	}
}
