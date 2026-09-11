package com.neatly.hotel.service;

import java.util.List;
import java.util.UUID;

import com.neatly.hotel.dto.CreateRoomRequest;
import com.neatly.hotel.dto.RoomResponse;

public interface RoomService {

	List<RoomResponse> findAll();

	RoomResponse findById(UUID id);

	RoomResponse create(CreateRoomRequest request);
}
