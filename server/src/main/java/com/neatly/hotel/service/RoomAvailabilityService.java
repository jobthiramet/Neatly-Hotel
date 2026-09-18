package com.neatly.hotel.service;

import java.util.List;

import com.neatly.hotel.dto.AvailableRoomResponse;
import com.neatly.hotel.dto.RoomAvailabilityQuery;

public interface RoomAvailabilityService {

	/** Room types with enough free, bookable units for the whole stay. Empty when nothing fits. */
	List<AvailableRoomResponse> search(RoomAvailabilityQuery query);
}
