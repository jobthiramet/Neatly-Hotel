package com.neatly.hotel.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neatly.hotel.dto.CreateRoomRequest;
import com.neatly.hotel.dto.RoomResponse;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.exception.ResourceNotFoundException;
import com.neatly.hotel.model.Room;
import com.neatly.hotel.repository.RoomRepository;

@Service
@Transactional
public class RoomServiceImpl implements RoomService {

	private final RoomRepository roomRepository;

	public RoomServiceImpl(RoomRepository roomRepository) {
		this.roomRepository = roomRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoomResponse> findAll() {
		return roomRepository.findAll().stream()
				.sorted(Comparator.comparing(Room::getRoomNumber, String.CASE_INSENSITIVE_ORDER))
				.map(RoomResponse::from)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public RoomResponse findById(UUID id) {
		return roomRepository.findById(id)
				.map(RoomResponse::from)
				.orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
	}

	@Override
	public RoomResponse create(CreateRoomRequest request) {
		String roomNumber = request.roomNumber().trim();
		String roomType = request.roomType().trim();
		String bedType = request.bedType().trim();
		String status = request.status().trim();

		if (!Room.ALLOWED_STATUSES.contains(status)) {
			throw new ApiException("Invalid room status: " + status, HttpStatus.BAD_REQUEST);
		}
		if (roomRepository.existsByRoomNumberIgnoreCase(roomNumber)) {
			throw new ApiException("Room number already exists: " + roomNumber, HttpStatus.CONFLICT);
		}

		Room room = new Room();
		room.setRoomNumber(roomNumber);
		room.setRoomType(roomType);
		room.setBedType(bedType);
		room.setStatus(status);
		room.setPricePerNight(BigDecimal.ZERO);
		room.setCapacity(2);
		room.setActive(true);
		return RoomResponse.from(roomRepository.save(room));
	}
}
