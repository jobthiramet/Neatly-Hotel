package com.neatly.hotel.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neatly.hotel.dto.CreateRoomRequest;
import com.neatly.hotel.dto.RoomResponse;
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
		return roomRepository.findAll().stream().map(RoomResponse::from).toList();
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
		Room room = new Room();
		room.setName(request.name());
		room.setType(request.type());
		room.setPricePerNight(request.pricePerNight());
		room.setCapacity(request.capacity());
		room.setActive(true);
		return RoomResponse.from(roomRepository.save(room));
	}
}
