package com.neatly.hotel.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.hotel.model.RoomStatus;

public interface RoomStatusRepository extends JpaRepository<RoomStatus, UUID> {

	List<RoomStatus> findAllByOrderBySortOrderAsc();

	Optional<RoomStatus> findByCodeIgnoreCase(String code);
}
