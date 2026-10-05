package com.neatly.hotel.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.hotel.model.Amenity;

public interface AmenityRepository extends JpaRepository<Amenity, UUID> {

	Optional<Amenity> findByNameIgnoreCase(String name);
}
