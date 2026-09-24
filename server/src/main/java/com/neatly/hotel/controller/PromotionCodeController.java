package com.neatly.hotel.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.hotel.dto.ApiResponse;
import com.neatly.hotel.dto.PromotionCodePreviewResponse;
import com.neatly.hotel.dto.PromotionCodeRequest;
import com.neatly.hotel.dto.PromotionCodeResponse;
import com.neatly.hotel.service.PromotionCodeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/promotion-codes")
@Tag(name = "Promotion codes")
public class PromotionCodeController {

	private final PromotionCodeService promotionCodeService;

	public PromotionCodeController(PromotionCodeService promotionCodeService) {
		this.promotionCodeService = promotionCodeService;
	}

	@GetMapping
	@SecurityRequirement(name = "clerkBearer")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid Clerk token")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Profile is missing or role is not agent")
	@Operation(summary = "List promo codes", description = "Admin Promo code. Non-deleted codes, sorted by code. Requires a Clerk session and agent role.")
	public ApiResponse<List<PromotionCodeResponse>> list() {
		return ApiResponse.ok(promotionCodeService.findAll());
	}

	@GetMapping("/preview")
	@Operation(summary = "Preview a promo code", description = "Public checkout check for one code, one room type, and a pre-discount purchase total. Does not list other codes.")
	public ApiResponse<PromotionCodePreviewResponse> preview(
			@RequestParam String code,
			@RequestParam UUID roomTypeId,
			@RequestParam BigDecimal purchase) {
		return ApiResponse.ok(promotionCodeService.preview(code, roomTypeId, purchase));
	}

	@GetMapping("/{id}")
	@SecurityRequirement(name = "clerkBearer")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid Clerk token")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Profile is missing or role is not agent")
	@Operation(summary = "Get promo code by id", description = "Requires a Clerk session and agent role.")
	public ApiResponse<PromotionCodeResponse> getById(@PathVariable UUID id) {
		return ApiResponse.ok(promotionCodeService.findById(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@SecurityRequirement(name = "clerkBearer")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid Clerk token")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Profile is missing or role is not agent")
	@Operation(summary = "Create promo code", description = "Requires a Clerk session and agent role. An empty roomTypeIds list applies the code to every room type.")
	public ApiResponse<PromotionCodeResponse> create(@Valid @RequestBody PromotionCodeRequest request) {
		return ApiResponse.ok("Promo code created", promotionCodeService.create(request));
	}

	@PutMapping("/{id}")
	@SecurityRequirement(name = "clerkBearer")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid Clerk token")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Profile is missing or role is not agent")
	@Operation(summary = "Update promo code", description = "Requires a Clerk session and agent role.")
	public ApiResponse<PromotionCodeResponse> update(
			@PathVariable UUID id,
			@Valid @RequestBody PromotionCodeRequest request) {
		return ApiResponse.ok("Promo code updated", promotionCodeService.update(id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@SecurityRequirement(name = "clerkBearer")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid Clerk token")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Profile is missing or role is not agent")
	@Operation(summary = "Soft-delete promo code", description = "Requires a Clerk session and agent role. Sets deletedAt. Past bookings keep the row.")
	public void delete(@PathVariable UUID id) {
		promotionCodeService.delete(id);
	}
}
