package com.neatly.hotel.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.hotel.model.RoomUnit;

public interface RoomUnitRepository extends JpaRepository<RoomUnit, UUID> {

	@EntityGraph(attributePaths = { "roomType", "roomStatus" })
	List<RoomUnit> findByDeletedAtIsNullOrderByRoomNumberAsc();

	@EntityGraph(attributePaths = { "roomType", "roomStatus" })
	Optional<RoomUnit> findByIdAndDeletedAtIsNull(UUID id);

	boolean existsByRoomNumber(String roomNumber);

	boolean existsByRoomNumberAndIdNot(String roomNumber, UUID id);
}
