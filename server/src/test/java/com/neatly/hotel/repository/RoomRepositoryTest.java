package com.neatly.hotel.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.neatly.hotel.model.BedType;
import com.neatly.hotel.model.Room;

@DataJpaTest
class RoomRepositoryTest {

	@Autowired
	private RoomRepository repository;

	private Room deleted;

	@BeforeEach
	void setUp() {
		repository.deleteAll(); // the local profile seeds sample rooms
		repository.save(room("Superior Garden View", BedType.DOUBLE, null));
		repository.save(room("Deluxe", BedType.KING, null));
		repository.save(room("Suite", BedType.TWIN, null));
		deleted = repository.save(room("Old Superior", BedType.SINGLE, Instant.now()));
		repository.flush();
	}

	@Test
	void searchMatchesRoomTypeOrBedTypeAndSkipsDeleted() {
		assertEquals(List.of("Superior Garden View"), names(repository.searchActive("superior", page(10))));
		assertEquals(List.of("Deluxe"), names(repository.searchActive("king", page(10))));
		assertEquals(3, repository.searchActive("", page(10)).getTotalElements());
	}

	@Test
	void searchPaginates() {
		Page<Room> first = repository.searchActive("", page(2));

		assertEquals(2, first.getContent().size());
		assertEquals(2, first.getTotalPages());
	}

	@Test
	void deletedRoomIsNotFoundAndItsNameCanBeReused() {
		assertTrue(repository.findByIdAndDeletedAtIsNull(deleted.getId()).isEmpty());
		assertTrue(repository.findById(deleted.getId()).isPresent());
		assertFalse(repository.existsByNameIgnoreCaseAndDeletedAtIsNull("old superior"));
		assertTrue(repository.existsByNameIgnoreCaseAndDeletedAtIsNull("DELUXE"));
	}

	private static PageRequest page(int size) {
		return PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));
	}

	private static List<String> names(Page<Room> page) {
		return page.getContent().stream().map(Room::getName).toList();
	}

	private static Room room(String name, BedType bedType, Instant deletedAt) {
		Room room = new Room();
		room.setName(name);
		room.setBedType(bedType);
		room.setSizeSqm(30);
		room.setCapacity(2);
		room.setPricePerNight(new BigDecimal("3000.00"));
		room.setDescription("Room");
		room.setAmenities(List.of("Shower"));
		room.setDeletedAt(deletedAt);
		return room;
	}
}
