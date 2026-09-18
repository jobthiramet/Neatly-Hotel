package com.neatly.hotel.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.neatly.hotel.model.BedType;
import com.neatly.hotel.model.Booking;
import com.neatly.hotel.model.BookingPaymentMethod;
import com.neatly.hotel.model.BookingRoom;
import com.neatly.hotel.model.BookingStatus;
import com.neatly.hotel.model.RoomStatus;
import com.neatly.hotel.model.RoomType;
import com.neatly.hotel.model.RoomUnit;

import jakarta.persistence.EntityManager;

/** The availability query behind GET /api/rooms/available. */
@DataJpaTest
class RoomTypeAvailabilityTest {

	private static final LocalDate IN = LocalDate.of(2030, 1, 10);
	private static final LocalDate OUT = LocalDate.of(2030, 1, 12);
	private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");
	private static final List<BookingStatus> HOLDING = List.of(
			BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);
	private static final List<String> BLOCKED = List.of("OUT_OF_ORDER", "OUT_OF_SERVICE");

	@Autowired
	private RoomTypeRepository repository;

	@Autowired
	private RoomStatusRepository statusRepository;

	@Autowired
	private EntityManager entityManager;

	private RoomType twin;
	private RoomType family;
	private int roomNumber;
	private int bookingNumber;

	@BeforeEach
	void setUp() {
		// the local profile seeds rooms and units; start from a known set
		for (String table : new String[] { "booking_rooms", "booking_items", "payments", "bookings", "room_units" }) {
			entityManager.createNativeQuery("delete from " + table).executeUpdate();
		}
		repository.deleteAll();

		twin = repository.save(room("Twin", 2, null));
		family = repository.save(room("Family", 4, null));
		RoomType deleted = repository.save(room("Old", 4, Instant.now()));

		unit(twin, "CLEAN");
		unit(twin, "DIRTY"); // housekeeping status: still bookable
		unit(twin, "CLEAN_INSPECTED");
		unit(twin, "OUT_OF_ORDER");
		unit(twin, "OUT_OF_SERVICE");
		unit(family, "CLEAN");
		unit(deleted, "CLEAN");
		entityManager.flush();
	}

	@Test
	void noBookingsCountsBookableUnitsAndSkipsDeletedTypes() {
		assertEquals(Map.of(twin.getId(), 3L, family.getId(), 1L), available(1, 1));
		// checkout counts the same inventory
		assertEquals(3L, repository.countBookableUnits(twin.getId(), BLOCKED));
	}

	@Test
	void overlappingBookingsReduceAvailability() {
		book(twin, IN.minusDays(1), IN.plusDays(1), BookingStatus.CONFIRMED, null);
		book(twin, OUT.minusDays(1), OUT.plusDays(3), BookingStatus.CHECKED_IN, null);
		book(family, IN, OUT, BookingStatus.PENDING_PAYMENT, NOW.plus(15, ChronoUnit.MINUTES));

		assertEquals(Map.of(twin.getId(), 1L, family.getId(), 0L), available(1, 1));
	}

	@Test
	void checkoutDayCanBeTheNextCheckInDay() {
		book(family, IN.minusDays(3), IN, BookingStatus.CONFIRMED, null);
		book(family, OUT, OUT.plusDays(2), BookingStatus.CONFIRMED, null);

		assertEquals(1L, available(1, 1).get(family.getId()));
	}

	@Test
	void cancelledExpiredAndLapsedHoldsDoNotBlock() {
		book(family, IN, OUT, BookingStatus.CANCELLED, null);
		book(family, IN, OUT, BookingStatus.EXPIRED, null);
		book(family, IN, OUT, BookingStatus.PENDING_PAYMENT, NOW.minus(1, ChronoUnit.MINUTES));

		assertEquals(1L, available(1, 1).get(family.getId()));
	}

	@Test
	void guestsMustFitInTheRequestedRooms() {
		// 2 rooms × capacity 2 fits 4 guests; 5 guests needs Family (capacity 4 × 2 rooms)
		assertEquals(Map.of(twin.getId(), 3L, family.getId(), 1L), available(2, 4));
		assertEquals(Map.of(family.getId(), 1L), available(2, 5));
		assertEquals(Map.of(family.getId(), 1L), available(1, 3));
	}

	private Map<UUID, Long> available(int rooms, int guests) {
		return repository.availability(IN, OUT, rooms, guests, NOW, HOLDING, BLOCKED).stream()
				.collect(Collectors.toMap(RoomTypeAvailability::roomTypeId, RoomTypeAvailability::availableUnits));
	}

	private void unit(RoomType type, String statusCode) {
		RoomStatus status = statusRepository.findByCodeIgnoreCase(statusCode).orElseThrow();
		RoomUnit unit = new RoomUnit();
		unit.setRoomNumber(String.format("%04d", 9000 + ++roomNumber));
		unit.setFloor((short) 9);
		unit.setRoomType(type);
		unit.setRoomStatus(status);
		entityManager.persist(unit);
	}

	private void book(RoomType type, LocalDate checkIn, LocalDate checkOut, BookingStatus status, Instant holdExpiresAt) {
		Booking booking = new Booking();
		booking.setBookingNumber("T" + ++bookingNumber);
		booking.setUserId("user_test");
		booking.setCheckIn(checkIn);
		booking.setCheckOut(checkOut);
		booking.setGuests(1);
		booking.setStatus(status);
		booking.setHoldExpiresAt(holdExpiresAt);
		booking.setPaymentMethod(BookingPaymentMethod.CASH);
		booking.setGuestFirstName("Test");
		booking.setGuestLastName("Guest");
		booking.setGuestEmail("guest@example.com");
		booking.setGuestPhone("0800000000");
		booking.setGuestCountry("Thailand");
		booking.setGuestDateOfBirth(LocalDate.of(1990, 1, 1));
		booking.setRoomSubtotal(BigDecimal.ZERO);
		booking.setExtrasTotal(BigDecimal.ZERO);
		booking.setDiscountTotal(BigDecimal.ZERO);
		booking.setGrandTotal(BigDecimal.ZERO);
		booking.setRoomNameSnapshot(type.getName());
		entityManager.persist(booking);

		BookingRoom room = new BookingRoom();
		room.setBooking(booking);
		room.setRoomType(type);
		room.setPricePerNight(BigDecimal.ZERO);
		entityManager.persist(room);
		entityManager.flush();
	}

	private static RoomType room(String name, int capacity, Instant deletedAt) {
		RoomType room = new RoomType();
		room.setName(name);
		room.setBedType(BedType.DOUBLE);
		room.setSizeSqm(30);
		room.setCapacity(capacity);
		room.setPricePerNight(new BigDecimal("3000.00"));
		room.setDescription("Room");
		room.setDeletedAt(deletedAt);
		return room;
	}
}
