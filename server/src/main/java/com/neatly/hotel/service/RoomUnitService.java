package com.neatly.hotel.service;

import java.util.List;
import java.util.UUID;

import com.neatly.hotel.dto.RoomStatusResponse;
import com.neatly.hotel.dto.RoomUnitRequest;
import com.neatly.hotel.dto.RoomUnitResponse;

public interface RoomUnitService {

	List<RoomUnitResponse> findAll();

	RoomUnitResponse findById(UUID id);

	RoomUnitResponse create(RoomUnitRequest request);

	RoomUnitResponse update(UUID id, RoomUnitRequest request);

	void delete(UUID id);

	List<RoomStatusResponse> listStatuses();
}
