package com.neatly.hotel.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.neatly.hotel.dto.ApiResponse;
import com.neatly.hotel.dto.CreateRoomRequest;
import com.neatly.hotel.dto.RoomResponse;
import com.neatly.hotel.service.RoomService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms")
public class RoomController {

	private final RoomService roomService;

	public RoomController(RoomService roomService) {
		this.roomService = roomService;
	}

	@GetMapping
	@Operation(summary = "List rooms")
	public ApiResponse<List<RoomResponse>> list() {
		return ApiResponse.ok(roomService.findAll());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get room by id")
	public ApiResponse<RoomResponse> getById(@PathVariable UUID id) {
		return ApiResponse.ok(roomService.findById(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create room")
	public ApiResponse<RoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
		return ApiResponse.ok("Room created", roomService.create(request));
	}
}
