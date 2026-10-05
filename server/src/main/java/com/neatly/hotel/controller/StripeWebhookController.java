package com.neatly.hotel.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.service.BookingService;

import io.swagger.v3.oas.annotations.Hidden;

@RestController
@RequestMapping("/api/stripe/webhooks")
@Hidden
public class StripeWebhookController {

	private final BookingService bookingService;

	public StripeWebhookController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> handle(
			@RequestHeader(name = "Stripe-Signature", required = false) String signature,
			@RequestBody String payload) {
		if (signature == null || signature.isBlank()) {
			throw new ApiException("Missing Stripe-Signature header", org.springframework.http.HttpStatus.BAD_REQUEST);
		}
		bookingService.handleStripeEvent(payload, signature);
		return ResponseEntity.ok("{}");
	}
}
