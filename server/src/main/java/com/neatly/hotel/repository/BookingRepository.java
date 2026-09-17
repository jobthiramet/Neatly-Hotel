package com.neatly.hotel.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.neatly.hotel.model.Booking;
import com.neatly.hotel.model.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

	Page<Booking> findByUserIdAndStatusInOrderByCreatedAtDesc(
			String userId,
			List<BookingStatus> statuses,
			Pageable pageable);

	Optional<Booking> findByIdAndUserId(UUID id, String userId);

	List<Booking> findByUserIdAndStatus(String userId, BookingStatus status);

	@Query("""
			select count(r) from BookingRoom r
			join r.booking b
			where r.roomType.id = :roomTypeId
			  and b.status in :statuses
			  and b.checkIn < :checkOut
			  and b.checkOut > :checkIn
			  and (
			    b.status <> com.neatly.hotel.model.BookingStatus.PENDING_PAYMENT
			    or b.holdExpiresAt is null
			    or b.holdExpiresAt > :now
			  )
			  and (:excludeId is null or b.id <> :excludeId)
			""")
	long occupiedUnits(
			@Param("roomTypeId") UUID roomTypeId,
			@Param("checkIn") LocalDate checkIn,
			@Param("checkOut") LocalDate checkOut,
			@Param("now") Instant now,
			@Param("excludeId") UUID excludeId,
			@Param("statuses") List<BookingStatus> statuses);
}
