package com.neatly.hotel.controller;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.hotel.dto.ApiResponse;
import com.neatly.hotel.dto.RoomStatusResponse;
import com.neatly.hotel.dto.RoomUnitRequest;
import com.neatly.hotel.dto.RoomUnitResponse;
import com.neatly.hotel.service.RoomUnitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/room-units")
@Tag(name = "Room units")
public class RoomUnitController {

	private final RoomUnitService roomUnitService;

	public RoomUnitController(RoomUnitService roomUnitService) {
		this.roomUnitService = roomUnitService;
	}

	@GetMapping
	@Operation(summary = "List room units", description = "Admin Room Management. Non-deleted units, sorted by room number.")
	public ApiResponse<List<RoomUnitResponse>> list() {
		return ApiResponse.ok(roomUnitService.findAll());
	}

	@GetMapping("/statuses")
	@Operation(summary = "List room statuses", description = "Housekeeping status codes for Room Management forms.")
	public ApiResponse<List<RoomStatusResponse>> listStatuses() {
		return ApiResponse.ok(roomUnitService.listStatuses());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get room unit by id")
	public ApiResponse<RoomUnitResponse> getById(@PathVariable UUID id) {
		return ApiResponse.ok(roomUnitService.findById(id));
	}

	// TODO(auth): restrict to admins once auth is wired.
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create room unit")
	public ApiResponse<RoomUnitResponse> create(@Valid @RequestBody RoomUnitRequest request) {
		return ApiResponse.ok("Room unit created", roomUnitService.create(request));
	}

	// TODO(auth): restrict to admins once auth is wired.
	@PutMapping("/{id}")
	@Operation(summary = "Update room unit")
	public ApiResponse<RoomUnitResponse> update(
			@PathVariable UUID id,
			@Valid @RequestBody RoomUnitRequest request) {
		return ApiResponse.ok("Room unit updated", roomUnitService.update(id, request));
	}

	// TODO(auth): restrict to admins once auth is wired.
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Soft-delete room unit")
	public void delete(@PathVariable UUID id) {
		roomUnitService.delete(id);
	}
}
