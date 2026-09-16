package com.neatly.hotel.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.hotel.model.RoomImage;

public interface RoomImageRepository extends JpaRepository<RoomImage, UUID> {
}
