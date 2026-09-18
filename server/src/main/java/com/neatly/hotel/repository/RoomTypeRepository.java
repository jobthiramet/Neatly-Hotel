package com.neatly.hotel.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.neatly.hotel.model.BookingStatus;
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

	/**
	 * One row per non-deleted room type that fits {@code guests} in {@code rooms} rooms.
	 * Bookable units: non-deleted units whose status is not in {@code blockedStatuses}.
	 * Booked units: booking rows of that type whose stay overlaps [checkIn, checkOut) and whose booking
	 * still holds the room (same rule as checkout). Bookings name a room type, not a unit, until a unit
	 * is assigned, so booked units are subtracted per type.
	 */
	@Query("""
			select new com.neatly.hotel.repository.RoomTypeAvailability(
			  t.id,
			  count(u),
			  (select count(r) from BookingRoom r join r.booking b
			   where r.roomType.id = t.id
			     and b.status in :holdingStatuses
			     and b.checkIn < :checkOut
			     and b.checkOut > :checkIn
			     and (b.status <> com.neatly.hotel.model.BookingStatus.PENDING_PAYMENT
			          or b.holdExpiresAt is null
			          or b.holdExpiresAt > :now)))
			from RoomUnit u join u.roomType t join u.roomStatus s
			where t.deletedAt is null
			  and u.deletedAt is null
			  and s.code not in :blockedStatuses
			  and t.capacity * :rooms >= :guests
			group by t.id
			""")
	List<RoomTypeAvailability> availability(
			@Param("checkIn") LocalDate checkIn,
			@Param("checkOut") LocalDate checkOut,
			@Param("rooms") int rooms,
			@Param("guests") int guests,
			@Param("now") Instant now,
			@Param("holdingStatuses") Collection<BookingStatus> holdingStatuses,
			@Param("blockedStatuses") Collection<String> blockedStatuses);

	@EntityGraph(attributePaths = "images")
	List<RoomType> findByIdIn(Collection<UUID> ids);

	Optional<RoomType> findByIdAndDeletedAtIsNull(UUID id);

	boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

	boolean existsByNameIgnoreCaseAndDeletedAtIsNullAndIdNot(String name, UUID id);
}
