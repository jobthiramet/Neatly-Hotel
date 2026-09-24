package com.neatly.hotel.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.hotel.model.PromotionCode;

public interface PromotionCodeRepository extends JpaRepository<PromotionCode, UUID> {

	@EntityGraph(attributePaths = "roomTypes")
	List<PromotionCode> findByDeletedAtIsNullOrderByCodeAsc();

	@EntityGraph(attributePaths = "roomTypes")
	Optional<PromotionCode> findByIdAndDeletedAtIsNull(UUID id);

	@EntityGraph(attributePaths = "roomTypes")
	Optional<PromotionCode> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);

	boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

	boolean existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(String code, UUID id);
}
