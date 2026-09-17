package com.neatly.hotel.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.neatly.hotel.model.RoomType;

public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {

	/** Non-deleted rooms whose name or bed type contains {@code search} (case-insensitive). */
	@Query("""
			select r from RoomType r
			where r.deletedAt is null
			  and (:search = '' or lower(r.name) like concat('%', :search, '%')
			       or lower(cast(r.bedType as string)) like concat('%', :search, '%'))
			""")
	Page<RoomType> searchActive(@Param("search") String search, Pageable pageable);

	Optional<RoomType> findByIdAndDeletedAtIsNull(UUID id);

	boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

	boolean existsByNameIgnoreCaseAndDeletedAtIsNullAndIdNot(String name, UUID id);
}
