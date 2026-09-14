package com.neatly.hotel.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.hotel.model.HotelInfo;

public interface HotelInfoRepository extends JpaRepository<HotelInfo, UUID> {

	Optional<HotelInfo> findFirstByOrderByCreatedAtAsc();
}
