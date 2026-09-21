package com.neatly.hotel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.neatly.hotel.dto.AvailableRoomResponse;
import com.neatly.hotel.dto.RoomAvailabilityQuery;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.model.BedType;
import com.neatly.hotel.model.RoomType;
import com.neatly.hotel.repository.RoomTypeAvailability;
import com.neatly.hotel.repository.RoomTypeRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class RoomAvailabilityServiceImplTest {

	private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");
	// 2030-01-10 00:30 in Bangkok is still 2030-01-09 in UTC
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2030-01-09T17:30:00Z"), BANGKOK);
	private static final LocalDate TODAY = LocalDate.of(2030, 1, 10);

	private final RoomTypeRepository repository = mock(RoomTypeRepository.class);
	private final RoomAvailabilityServiceImpl service = new RoomAvailabilityServiceImpl(repository, CLOCK, 30);
	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void returnsTypesWithEnoughFreeUnitsCheapestFirst() {
		RoomType cheap = room("Superior", "2500");
		RoomType pricey = room("Suite", "9000");
		RoomType full = room("Deluxe", "4000");
		when(repository.availability(any(), any(), anyInt(), anyInt(), any(), any(), any(), anyBoolean(), any())).thenReturn(List.of(
				new RoomTypeAvailability(pricey.getId(), 3, 1),
				new RoomTypeAvailability(cheap.getId(), 5, 0),
				new RoomTypeAvailability(full.getId(), 4, 3)));
		when(repository.findByIdIn(Set.of(pricey.getId(), cheap.getId()))).thenReturn(List.of(pricey, cheap));

		List<AvailableRoomResponse> result = service.search(query(TODAY, TODAY.plusDays(2), 2, 2));

		assertEquals(List.of("Superior", "Suite"), result.stream().map(AvailableRoomResponse::name).toList());
		assertEquals(5, result.getFirst().availableUnits());
		assertEquals("Superior room", result.getFirst().description());
	}

	@Test
	void roomTypeIdsAreForwardedAndAbsenceMeansEveryType() {
		RoomType picked = room("Suite", "9000");
		when(repository.availability(any(), any(), anyInt(), anyInt(), any(), any(), any(), anyBoolean(), any()))
				.thenReturn(List.of(new RoomTypeAvailability(picked.getId(), 2, 0)));
		when(repository.findByIdIn(Set.of(picked.getId()))).thenReturn(List.of(picked));

		// no ids: the query is told to ignore the filter
		service.search(query(TODAY, TODAY.plusDays(1), 1, 1));
		verify(repository).availability(any(), any(), anyInt(), anyInt(), any(), any(), any(), eq(true), any());

		// one id: forwarded as the filter, and only that type comes back
		List<AvailableRoomResponse> result = service.search(
				new RoomAvailabilityQuery(TODAY, TODAY.plusDays(1), 1, 1, List.of(picked.getId())));
		assertEquals(List.of("Suite"), result.stream().map(AvailableRoomResponse::name).toList());
		verify(repository).availability(any(), any(), anyInt(), anyInt(), any(), any(), any(), eq(false),
				eq(List.of(picked.getId())));

		// an unknown id matches nothing: the query returns no rows, so the page is empty
		when(repository.availability(any(), any(), anyInt(), anyInt(), any(), any(), any(), anyBoolean(), any()))
				.thenReturn(List.of());
		assertTrue(service.search(new RoomAvailabilityQuery(TODAY, TODAY.plusDays(1), 1, 1, List.of(UUID.randomUUID())))
				.isEmpty());
	}

	@Test
	void nothingAvailableIsAnEmptyList() {
		when(repository.availability(any(), any(), anyInt(), anyInt(), any(), any(), any(), anyBoolean(), any())).thenReturn(List.of());

		assertTrue(service.search(query(TODAY, TODAY.plusDays(1), 1, 1)).isEmpty());
		verify(repository, never()).findByIdIn(any());
	}

	@Test
	void checkInBeforeTodayInHotelTimeIsRejected() {
		ApiException error = assertThrows(ApiException.class,
				() -> service.search(query(TODAY.minusDays(1), TODAY.plusDays(1), 1, 1)));

		assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
		assertTrue(error.getMessage().startsWith("checkIn:"));
	}

	@Test
	void staysLongerThanTheMaximumAreRejected() {
		service.search(query(TODAY, TODAY.plusDays(30), 1, 1));

		ApiException error = assertThrows(ApiException.class,
				() -> service.search(query(TODAY, TODAY.plusDays(31), 1, 1)));
		assertTrue(error.getMessage().startsWith("checkOut:"));
	}

	@Test
	void queryValidationCoversDatesAndCaps() {
		assertEquals(Set.of("checkOutValid"), fields(query(TODAY, TODAY, 1, 1)));
		assertEquals(Set.of("rooms", "guests"), fields(query(TODAY, TODAY.plusDays(1), 0, 7)));
		assertEquals(Set.of("rooms"), fields(query(TODAY, TODAY.plusDays(1), 11, 1)));
		assertEquals(Set.of("checkIn", "checkOut", "rooms", "guests"), fields(new RoomAvailabilityQuery(null, null, null, null, null)));
		assertEquals(Set.of("roomTypeIds"), fields(new RoomAvailabilityQuery(TODAY, TODAY.plusDays(1), 1, 1,
				java.util.stream.Stream.generate(UUID::randomUUID).limit(21).toList())));
		assertTrue(fields(query(TODAY, TODAY.plusDays(1), 10, 6)).isEmpty());
	}

	private Set<String> fields(RoomAvailabilityQuery query) {
		return validator.validate(query).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(java.util.stream.Collectors.toSet());
	}

	private static RoomAvailabilityQuery query(LocalDate checkIn, LocalDate checkOut, int rooms, int guests) {
		return new RoomAvailabilityQuery(checkIn, checkOut, rooms, guests, null);
	}

	private static RoomType room(String name, String price) {
		RoomType room = new RoomType();
		room.setId(UUID.randomUUID());
		room.setName(name);
		room.setBedType(BedType.DOUBLE);
		room.setSizeSqm(30);
		room.setCapacity(2);
		room.setPricePerNight(new BigDecimal(price));
		room.setDescription(name + " room");
		return room;
	}
}
