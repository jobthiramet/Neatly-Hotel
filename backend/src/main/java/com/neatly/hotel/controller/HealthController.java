package com.neatly.hotel.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Health")
public class HealthController {

	@GetMapping("/health")
	@Operation(summary = "Health check")
	public Map<String, String> health() {
		return Map.of(
				"status", "ok",
				"service", "neatly-hotel-api");
	}
}
