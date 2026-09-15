package com.neatly.hotel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.neatly.hotel.dto.ReorderRoomImagesRequest;
import com.neatly.hotel.dto.RoomRequest;
import com.neatly.hotel.dto.RoomResponse;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.exception.ResourceNotFoundException;
import com.neatly.hotel.model.BedType;
import com.neatly.hotel.model.Room;
import com.neatly.hotel.model.RoomImage;
import com.neatly.hotel.repository.RoomImageRepository;
import com.neatly.hotel.repository.RoomRepository;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class RoomServiceImplTest {

	private static final String BUCKET = "room-images";
	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0 };

	private final RoomRepository repository = mock(RoomRepository.class);
	private final RoomImageRepository imageRepository = mock(RoomImageRepository.class);
	private final StorageService storage = mock(StorageService.class);
	private final RoomServiceImpl service = new RoomServiceImpl(repository, imageRepository, storage, BUCKET);

	@BeforeEach
	void setUp() {
		when(repository.saveAndFlush(any(Room.class))).thenAnswer(call -> {
			Room room = call.getArgument(0);
			if (room.getId() == null) {
				room.setId(UUID.randomUUID());
			}
			return room;
		});
		when(storage.uploadOrReplace(eq(BUCKET), anyString(), any(), anyString()))
				.thenAnswer(call -> "https://x.supabase.co/storage/v1/object/public/room-images/" + call.getArgument(1));
	}

	@Test
	void createSavesFieldsAndUploadsMainAndGallery() {
		RoomResponse response = service.create(request("  Deluxe  ", null), png(), gallery(4));

		assertEquals("Deluxe", response.name());
		assertNotNull(response.mainImage());
		assertEquals(4, response.gallery().size());
		verify(storage, times(5)).uploadOrReplace(eq(BUCKET), startsWith("rooms/" + response.id() + "/"), any(), eq("image/png"));
	}

	@Test
	void createDeletesUploadedObjectsWhenAnUploadFails() {
		when(storage.uploadOrReplace(eq(BUCKET), anyString(), any(), anyString()))
				.thenReturn("https://x/1")
				.thenReturn("https://x/2")
				.thenThrow(new ApiException("Failed to upload file to storage", HttpStatus.BAD_GATEWAY));

		ApiException ex = assertThrows(ApiException.class, () -> service.create(request("Deluxe", null), png(), gallery(4)));

		assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
		verify(storage, times(2)).delete(eq(BUCKET), startsWith("rooms/"));
	}

	@Test
	void createRejectsTooFewGalleryImagesBeforeTouchingStorage() {
		ApiException ex = assertThrows(ApiException.class, () -> service.create(request("Deluxe", null), png(), gallery(3)));

		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
		verify(storage, never()).uploadOrReplace(anyString(), anyString(), any(), anyString());
	}

	@Test
	void createRejectsSvgDisguisedAsPng() {
		MockMultipartFile svg = new MockMultipartFile("mainImage", "x.png", "image/png", "<svg/>".getBytes());

		assertThrows(ApiException.class, () -> service.create(request("Deluxe", null), svg, gallery(4)));
	}

	@Test
	void createAndUpdateRejectDuplicateName() {
		UUID id = UUID.randomUUID();
		when(repository.existsByNameIgnoreCaseAndDeletedAtIsNull("Deluxe")).thenReturn(true);
		when(repository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(room(id, 4)));
		when(repository.existsByNameIgnoreCaseAndDeletedAtIsNullAndIdNot("Deluxe", id)).thenReturn(true);

		assertEquals(HttpStatus.CONFLICT,
				assertThrows(ApiException.class, () -> service.create(request("Deluxe", null), png(), gallery(4))).getStatus());
		assertEquals(HttpStatus.CONFLICT,
				assertThrows(ApiException.class, () -> service.update(id, request("Deluxe", null))).getStatus());
	}

	@Test
	void updateAppliesFields() {
		UUID id = UUID.randomUUID();
		when(repository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(room(id, 4)));

		RoomResponse response = service.update(id, request("Suite", new BigDecimal("2500.00")));

		assertEquals("Suite", response.name());
		assertEquals(new BigDecimal("2500.00"), response.promotionPrice());
		assertEquals(List.of("Shower"), response.amenities());
	}

	@Test
	void softDeleteSetsDeletedAtAndLaterLookupsAre404() {
		UUID id = UUID.randomUUID();
		Room room = room(id, 4);
		when(repository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(room)).thenReturn(Optional.empty());

		service.delete(id);

		assertNotNull(room.getDeletedAt());
		verify(storage, never()).delete(anyString(), anyString());
		assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
	}

	@Test
	void listClampsPageSizeAndLowercasesSearch() {
		when(repository.searchActive(eq("deluxe"), any(Pageable.class))).thenAnswer(call -> {
			Pageable pageable = call.getArgument(1);
			assertEquals(RoomServiceImpl.MAX_PAGE_SIZE, pageable.getPageSize());
			return new PageImpl<Room>(List.of(), pageable, 0);
		});

		assertEquals(0, service.list("  Deluxe ", -1, 500).totalElements());
	}

	@Test
	void promotionPriceMustBeLowerThanPrice() {
		Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

		assertTrue(validator.validate(request("Deluxe", new BigDecimal("2999.99"))).isEmpty());
		assertFalse(validator.validate(request("Deluxe", new BigDecimal("3000.00"))).isEmpty());
	}

	@Test
	void replacingMainImageDeletesOldObjectAfterSave() {
		UUID id = UUID.randomUUID();
		Room room = room(id, 4);
		RoomImage oldMain = room.getImages().getFirst();
		when(repository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(room));

		RoomResponse response = service.addImage(id, png(), true);

		assertTrue(response.mainImage().url().contains("rooms/" + id + "/"));
		verify(imageRepository).delete(oldMain);
		verify(storage).delete(BUCKET, oldMain.getStoragePath());
	}

	@Test
	void deleteImageKeepsAtLeastFourGalleryImages() {
		UUID id = UUID.randomUUID();
		Room room = room(id, 4);
		when(repository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(room));

		ApiException ex = assertThrows(ApiException.class, () -> service.deleteImage(id, room.getImages().get(1).getId()));

		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
	}

	@Test
	void reorderRequiresEveryGalleryImage() {
		UUID id = UUID.randomUUID();
		Room room = room(id, 4);
		when(repository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(room));
		List<UUID> galleryIds = room.getImages().stream().skip(1).map(RoomImage::getId).toList();

		RoomResponse response = service.reorderImages(id, new ReorderRoomImagesRequest(galleryIds.reversed()));

		assertEquals(galleryIds.reversed(), response.gallery().stream().map(image -> image.id()).toList());
		assertThrows(ApiException.class,
				() -> service.reorderImages(id, new ReorderRoomImagesRequest(galleryIds.subList(0, 3))));
	}

	private static RoomRequest request(String name, BigDecimal promotionPrice) {
		return new RoomRequest(name, BedType.DOUBLE, 32, 2, new BigDecimal("3000.00"), promotionPrice, "Nice room", List.of(" Shower "));
	}

	private static MockMultipartFile png() {
		return new MockMultipartFile("file", "photo.png", "image/png", PNG);
	}

	private static List<MultipartFile> gallery(int count) {
		List<MultipartFile> files = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			files.add(png());
		}
		return files;
	}

	/** A saved room with a main image followed by {@code galleryCount} gallery images. */
	private static Room room(UUID id, int galleryCount) {
		Room room = new Room();
		room.setId(id);
		room.setName("Deluxe");
		room.setBedType(BedType.DOUBLE);
		room.setSizeSqm(32);
		room.setPricePerNight(new BigDecimal("3000.00"));
		room.setDescription("Nice room");
		room.setAmenities(new ArrayList<>(List.of("Shower")));
		for (int i = 0; i <= galleryCount; i++) {
			RoomImage image = new RoomImage();
			image.setId(UUID.randomUUID());
			image.setRoom(room);
			image.setIsMain(i == 0);
			image.setSortOrder(i);
			image.setStoragePath("rooms/" + id + "/" + i + ".png");
			image.setUrl("https://x/" + i);
			room.getImages().add(image);
		}
		return room;
	}
}
