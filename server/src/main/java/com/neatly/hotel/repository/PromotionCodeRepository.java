package com.neatly.hotel.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.hotel.model.PromotionCode;

public interface PromotionCodeRepository extends JpaRepository<PromotionCode, java.util.UUID> {

	Optional<PromotionCode> findByCodeIgnoreCaseAndActiveTrue(String code);
}
