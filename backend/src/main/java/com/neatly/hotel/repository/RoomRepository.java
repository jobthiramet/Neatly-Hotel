package com.neatly.hotel.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.hotel.model.Room;

public interface RoomRepository extends JpaRepository<Room, UUID> {
}
