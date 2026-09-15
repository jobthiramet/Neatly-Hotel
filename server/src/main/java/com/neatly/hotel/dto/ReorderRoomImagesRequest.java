package com.neatly.hotel.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ReorderRoomImagesRequest(
		@Schema(description = "Every gallery image id of the room, in the new order")
		@NotEmpty List<@NotNull UUID> imageIds) {
}
