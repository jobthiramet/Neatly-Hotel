package com.neatly.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.hotel.model.Profile;

public interface ProfileRepository extends JpaRepository<Profile, String> {
}
