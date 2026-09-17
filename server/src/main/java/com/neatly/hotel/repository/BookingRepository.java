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

	Page<Booking> findByClerkUserIdAndStatusInOrderByCreatedAtDesc(
			String clerkUserId,
			List<BookingStatus> statuses,
			Pageable pageable);

	Optional<Booking> findByIdAndClerkUserId(UUID id, String clerkUserId);

	List<Booking> findByClerkUserIdAndStatus(String clerkUserId, BookingStatus status);

	@Query("""
			select coalesce(sum(b.roomsCount), 0) from Booking b
			where b.room.id = :roomId
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
			@Param("roomId") UUID roomId,
			@Param("checkIn") LocalDate checkIn,
			@Param("checkOut") LocalDate checkOut,
			@Param("now") Instant now,
			@Param("excludeId") UUID excludeId,
			@Param("statuses") List<BookingStatus> statuses);
}
