package com.neatly.hotel.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.neatly.hotel.dto.PageResponse;
import com.neatly.hotel.dto.ReorderRoomImagesRequest;
import com.neatly.hotel.dto.RoomRequest;
import com.neatly.hotel.dto.RoomResponse;
import com.neatly.hotel.dto.RoomSummaryResponse;

public interface RoomTypeService {

	PageResponse<RoomSummaryResponse> list(String search, int page, int size);

	RoomResponse findById(UUID id);

	RoomResponse create(RoomRequest request, MultipartFile mainImage, List<MultipartFile> gallery);

	RoomResponse update(UUID id, RoomRequest request);

	void delete(UUID id);

	RoomResponse addImage(UUID id, MultipartFile file, boolean main);

	RoomResponse deleteImage(UUID id, UUID imageId);

	RoomResponse reorderImages(UUID id, ReorderRoomImagesRequest request);
}
