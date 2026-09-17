package com.neatly.hotel.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neatly.hotel.dto.RoomStatusResponse;
import com.neatly.hotel.dto.RoomUnitRequest;
import com.neatly.hotel.dto.RoomUnitResponse;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.exception.ResourceNotFoundException;
import com.neatly.hotel.model.RoomStatus;
import com.neatly.hotel.model.RoomType;
import com.neatly.hotel.model.RoomUnit;
import com.neatly.hotel.repository.RoomStatusRepository;
import com.neatly.hotel.repository.RoomTypeRepository;
import com.neatly.hotel.repository.RoomUnitRepository;

@Service
@Transactional
public class RoomUnitServiceImpl implements RoomUnitService {

	private final RoomUnitRepository roomUnitRepository;
	private final RoomTypeRepository roomTypeRepository;
	private final RoomStatusRepository roomStatusRepository;

	public RoomUnitServiceImpl(
			RoomUnitRepository roomUnitRepository,
			RoomTypeRepository roomTypeRepository,
			RoomStatusRepository roomStatusRepository) {
		this.roomUnitRepository = roomUnitRepository;
		this.roomTypeRepository = roomTypeRepository;
		this.roomStatusRepository = roomStatusRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoomUnitResponse> findAll() {
		return roomUnitRepository.findByDeletedAtIsNullOrderByRoomNumberAsc().stream()
				.map(unit -> RoomUnitResponse.from(unit, false))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public RoomUnitResponse findById(UUID id) {
		return RoomUnitResponse.from(requireUnit(id), false);
	}

	@Override
	public RoomUnitResponse create(RoomUnitRequest request) {
		String roomNumber = request.roomNumber().trim();
		if (roomUnitRepository.existsByRoomNumber(roomNumber)) {
			throw new ApiException("Room number already exists: " + roomNumber, HttpStatus.CONFLICT);
		}

		RoomUnit unit = new RoomUnit();
		apply(unit, roomNumber, request.roomTypeId(), request.statusCode());
		return RoomUnitResponse.from(roomUnitRepository.save(unit), false);
	}

	@Override
	public RoomUnitResponse update(UUID id, RoomUnitRequest request) {
		RoomUnit unit = requireUnit(id);
		String roomNumber = request.roomNumber().trim();
		if (roomUnitRepository.existsByRoomNumberAndIdNot(roomNumber, id)) {
			throw new ApiException("Room number already exists: " + roomNumber, HttpStatus.CONFLICT);
		}
		apply(unit, roomNumber, request.roomTypeId(), request.statusCode());
		return RoomUnitResponse.from(roomUnitRepository.save(unit), false);
	}

	@Override
	public void delete(UUID id) {
		RoomUnit unit = requireUnit(id);
		unit.setDeletedAt(Instant.now());
		roomUnitRepository.save(unit);
	}

	@Override
	@Transactional(readOnly = true)
	public List<RoomStatusResponse> listStatuses() {
		return roomStatusRepository.findAllByOrderBySortOrderAsc().stream()
				.map(RoomStatusResponse::from)
				.toList();
	}

	private void apply(RoomUnit unit, String roomNumber, UUID roomTypeId, String statusCode) {
		RoomType roomType = roomTypeRepository.findByIdAndDeletedAtIsNull(roomTypeId)
				.orElseThrow(() -> new ResourceNotFoundException("Room type not found: " + roomTypeId));
		RoomStatus status = roomStatusRepository.findByCodeIgnoreCase(statusCode.trim())
				.orElseThrow(() -> new ApiException("Invalid room status: " + statusCode, HttpStatus.BAD_REQUEST));

		unit.setRoomNumber(roomNumber);
		unit.setFloor(floorFromRoomNumber(roomNumber));
		unit.setRoomType(roomType);
		unit.setRoomStatus(status);
	}

	private RoomUnit requireUnit(UUID id) {
		return roomUnitRepository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new ResourceNotFoundException("Room unit not found: " + id));
	}

	/** Same rule as the seed: 0001–0010 → floor 1, 0011–0020 → floor 2, … */
	static short floorFromRoomNumber(String roomNumber) {
		int n = Integer.parseInt(roomNumber);
		return (short) ((n - 1) / 10 + 1);
	}
}
