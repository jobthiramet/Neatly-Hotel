package com.neatly.hotel.controller;

import java.util.List;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.neatly.hotel.dto.ApiResponse;
import com.neatly.hotel.dto.AvailableRoomResponse;
import com.neatly.hotel.dto.PageResponse;
import com.neatly.hotel.dto.ReorderRoomImagesRequest;
import com.neatly.hotel.dto.RoomAvailabilityQuery;
import com.neatly.hotel.dto.RoomRequest;
import com.neatly.hotel.dto.RoomResponse;
import com.neatly.hotel.dto.RoomSummaryResponse;
import com.neatly.hotel.service.RoomAvailabilityService;
import com.neatly.hotel.service.RoomTypeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms")
public class RoomController {

	private final RoomTypeService roomService;
	private final RoomAvailabilityService availabilityService;

	public RoomController(RoomTypeService roomService, RoomAvailabilityService availabilityService) {
		this.roomService = roomService;
		this.availabilityService = availabilityService;
	}

	@GetMapping
	@Operation(
			summary = "List rooms (paginated)",
			description = "Public. Non-deleted rooms, newest first. `search` matches room type or bed type. "
					+ "`page` is zero-based; `size` defaults to 10, capped at 50. Rate limited per IP (429 + Retry-After).")
	public ApiResponse<PageResponse<RoomSummaryResponse>> list(
			@RequestParam(defaultValue = "") String search,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return ApiResponse.ok(roomService.list(search, page, size));
	}

	@GetMapping("/available")
	@Operation(
			summary = "Search available room types",
			description = "Public. Room types with at least `rooms` bookable units free for the whole stay "
					+ "[checkIn, checkOut) and `capacity × rooms ≥ guests`. A checkout day can be another stay's "
					+ "check-in day. Dates are ISO (YYYY-MM-DD); checkIn must not be before today in hotel time "
					+ "(Asia/Bangkok); stays are capped at 30 nights. Empty list when nothing is available. "
					+ "Rate limited per IP (429 + Retry-After).")
	public ApiResponse<List<AvailableRoomResponse>> available(@ParameterObject @Valid @ModelAttribute RoomAvailabilityQuery query) {
		return ApiResponse.ok(availabilityService.search(query));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get room by id", description = "Public. 404 for deleted rooms.")
	public ApiResponse<RoomResponse> getById(@PathVariable UUID id) {
		return ApiResponse.ok(roomService.findById(id));
	}

	// TODO(auth): restrict to admins once auth is wired.
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(
			summary = "Create room with images",
			description = "Multipart: `room` (application/json RoomRequest), `mainImage` (1 file), `gallery` (4–12 files). "
					+ "Images: PNG, JPEG or WEBP, max 5 MB each. Returns 503 when storage is not configured.")
	public ApiResponse<RoomResponse> create(
			@Valid @RequestPart("room") RoomRequest request,
			@RequestPart("mainImage") MultipartFile mainImage,
			@RequestPart("gallery") List<MultipartFile> gallery) {
		return ApiResponse.ok("Room created", roomService.create(request, mainImage, gallery));
	}

	// TODO(auth): restrict to admins once auth is wired.
	@PutMapping("/{id}")
	@Operation(summary = "Update room fields")
	public ApiResponse<RoomResponse> update(@PathVariable UUID id, @Valid @RequestBody RoomRequest request) {
		return ApiResponse.ok("Room updated", roomService.update(id, request));
	}

	// TODO(auth): restrict to admins once auth is wired.
	@DeleteMapping("/{id}")
	@Operation(summary = "Soft delete room", description = "Sets deletedAt. Images are kept in storage.")
	public ApiResponse<Void> delete(@PathVariable UUID id) {
		roomService.delete(id);
		return ApiResponse.ok("Room deleted", null);
	}

	// TODO(auth): restrict to admins once auth is wired.
	@PostMapping(path = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(
			summary = "Upload room image",
			description = "Multipart field `file`: PNG, JPEG or WEBP, max 5 MB. `main=true` replaces the main image "
					+ "(the old object is deleted); otherwise appends to the gallery (max 12).")
	public ApiResponse<RoomResponse> addImage(
			@PathVariable UUID id,
			@RequestPart("file") MultipartFile file,
			@RequestParam(defaultValue = "false") boolean main) {
		return ApiResponse.ok("Room image uploaded", roomService.addImage(id, file, main));
	}

	// TODO(auth): restrict to admins once auth is wired.
	@DeleteMapping("/{id}/images/{imageId}")
	@Operation(
			summary = "Remove gallery image",
			description = "Deletes the storage object too. 400 for the main image or when fewer than 4 gallery images would remain.")
	public ApiResponse<RoomResponse> deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
		return ApiResponse.ok("Room image removed", roomService.deleteImage(id, imageId));
	}

	// TODO(auth): restrict to admins once auth is wired.
	@PutMapping("/{id}/images/order")
	@Operation(summary = "Reorder gallery images", description = "`imageIds` must list every gallery image exactly once.")
	public ApiResponse<RoomResponse> reorderImages(
			@PathVariable UUID id,
			@Valid @RequestBody ReorderRoomImagesRequest request) {
		return ApiResponse.ok("Room images reordered", roomService.reorderImages(id, request));
	}
}
