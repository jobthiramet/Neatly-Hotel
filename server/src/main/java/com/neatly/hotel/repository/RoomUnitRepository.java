package com.neatly.hotel.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.neatly.hotel.model.RoomUnit;

public interface RoomUnitRepository extends JpaRepository<RoomUnit, UUID> {

	@EntityGraph(attributePaths = { "roomType", "roomStatus" })
	List<RoomUnit> findByDeletedAtIsNullOrderByRoomNumberAsc();

	@EntityGraph(attributePaths = { "roomType", "roomStatus" })
	Optional<RoomUnit> findByIdAndDeletedAtIsNull(UUID id);

	boolean existsByRoomNumber(String roomNumber);

	boolean existsByRoomNumberAndIdNot(String roomNumber, UUID id);

	@Query("""
			select count(u) from RoomUnit u
			where u.deletedAt is null and u.roomStatus.code not in :blockedStatuses
			""")
	long countBookable(@Param("blockedStatuses") List<String> blockedStatuses);
}
