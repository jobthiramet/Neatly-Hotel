package com.neatly.hotel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.neatly.hotel.dto.PromotionCodeRequest;
import com.neatly.hotel.dto.PromotionPreviewStatus;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.model.DiscountType;
import com.neatly.hotel.model.PromotionCode;
import com.neatly.hotel.model.RoomType;
import com.neatly.hotel.repository.PromotionCodeRepository;
import com.neatly.hotel.repository.RoomTypeRepository;

class PromotionCodeServiceImplTest {

	private final PromotionCodeRepository promotionCodeRepository = mock(PromotionCodeRepository.class);
	private final RoomTypeRepository roomTypeRepository = mock(RoomTypeRepository.class);
	private PromotionCodeServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new PromotionCodeServiceImpl(promotionCodeRepository, roomTypeRepository);
		when(promotionCodeRepository.save(any(PromotionCode.class))).thenAnswer(invocation -> {
			PromotionCode promo = invocation.getArgument(0);
			if (promo.getId() == null) {
				promo.setId(UUID.randomUUID());
			}
			return promo;
		});
	}

	@Test
	void createFixedCodeStoresUppercaseAndNoRoomLimit() {
		when(promotionCodeRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull("SAVE400")).thenReturn(false);

		var response = service.create(request("save400", DiscountType.FIXED, new BigDecimal("400"), null, BigDecimal.ZERO, List.of()));

		assertEquals("SAVE400", response.code());
		assertEquals(DiscountType.FIXED, response.discountType());
		assertEquals(new BigDecimal("400.00"), response.amountOff());
		assertNull(response.percentOff());
		assertEquals(List.of(), response.roomTypes());
	}

	@Test
	void createPercentCodeClearsFixedAmount() {
		when(promotionCodeRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull("SAVE10")).thenReturn(false);
		UUID typeId = UUID.randomUUID();
		RoomType type = new RoomType();
		type.setId(typeId);
		type.setName("Deluxe");
		when(roomTypeRepository.findAllById(any())).thenReturn(List.of(type));

		var response = service.create(request("SAVE10", DiscountType.PERCENT, null, new BigDecimal("10"), new BigDecimal("1000"), List.of(typeId)));

		assertEquals(DiscountType.PERCENT, response.discountType());
		assertNull(response.amountOff());
		assertEquals(new BigDecimal("10.00"), response.percentOff());
		assertEquals(new BigDecimal("1000.00"), response.minPurchaseAmount());
		assertEquals(1, response.roomTypes().size());
		assertEquals("Deluxe", response.roomTypes().get(0).name());
	}

	@Test
	void duplicateCodeIsRejected() {
		when(promotionCodeRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull("SAVE400")).thenReturn(true);

		ApiException error = assertThrows(ApiException.class,
				() -> service.create(request("SAVE400", DiscountType.FIXED, new BigDecimal("10"), null, BigDecimal.ZERO, null)));

		assertEquals(HttpStatus.CONFLICT, error.getStatus());
	}

	@Test
	void missingRoomTypeIsRejected() {
		when(promotionCodeRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull("SAVE400")).thenReturn(false);
		when(roomTypeRepository.findAllById(any())).thenReturn(List.of());

		ApiException error = assertThrows(ApiException.class,
				() -> service.create(request("SAVE400", DiscountType.FIXED, new BigDecimal("10"), null, BigDecimal.ZERO, List.of(UUID.randomUUID()))));

		assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
	}

	@Test
	void deleteSetsDeletedAt() {
		UUID id = UUID.randomUUID();
		PromotionCode promo = new PromotionCode();
		promo.setId(id);
		when(promotionCodeRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(promo));

		service.delete(id);

		assertNotNull(promo.getDeletedAt());
		verify(promotionCodeRepository).save(promo);
	}

	@Test
	void deletedRoomTypeCannotBeAttached() {
		when(promotionCodeRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull("SAVE400")).thenReturn(false);
		UUID typeId = UUID.randomUUID();
		RoomType type = new RoomType();
		type.setId(typeId);
		type.setDeletedAt(Instant.now());
		when(roomTypeRepository.findAllById(any())).thenReturn(List.of(type));

		ApiException error = assertThrows(ApiException.class,
				() -> service.create(request("SAVE400", DiscountType.FIXED, new BigDecimal("10"), null, BigDecimal.ZERO, List.of(typeId))));

		assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
	}

	@Test
	void previewUnknownCodeIsNotFound() {
		UUID roomTypeId = UUID.randomUUID();
		when(roomTypeRepository.findByIdAndDeletedAtIsNull(roomTypeId)).thenReturn(Optional.of(room(roomTypeId)));
		when(promotionCodeRepository.findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull("NOPE")).thenReturn(Optional.empty());

		var response = service.preview("nope", roomTypeId, new BigDecimal("2500"));

		assertEquals(PromotionPreviewStatus.NOT_FOUND, response.status());
		assertNull(response.discountAmount());
	}

	@Test
	void previewCodeForAnotherRoomIsNotEligible() {
		UUID bookedId = UUID.randomUUID();
		UUID otherId = UUID.randomUUID();
		when(roomTypeRepository.findByIdAndDeletedAtIsNull(bookedId)).thenReturn(Optional.of(room(bookedId)));
		PromotionCode promo = fixedPromo("SUITEONLY", new BigDecimal("400"));
		promo.getRoomTypes().add(room(otherId));
		when(promotionCodeRepository.findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull("SUITEONLY")).thenReturn(Optional.of(promo));

		var response = service.preview("SUITEONLY", bookedId, new BigDecimal("2500"));

		assertEquals(PromotionPreviewStatus.ROOM_NOT_ELIGIBLE, response.status());
		assertNull(response.discountAmount());
	}

	@Test
	void previewAppliesFixedDiscountWhenTheRoomIsIncluded() {
		UUID roomTypeId = UUID.randomUUID();
		when(roomTypeRepository.findByIdAndDeletedAtIsNull(roomTypeId)).thenReturn(Optional.of(room(roomTypeId)));
		when(promotionCodeRepository.findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull("NEATLYNEW400"))
				.thenReturn(Optional.of(fixedPromo("NEATLYNEW400", new BigDecimal("400"))));

		var response = service.preview("neatlynew400", roomTypeId, new BigDecimal("2500"));

		assertEquals(PromotionPreviewStatus.APPLIED, response.status());
		assertEquals(new BigDecimal("400.00"), response.discountAmount());
	}

	@Test
	void previewBelowMinimumDoesNotApply() {
		UUID roomTypeId = UUID.randomUUID();
		when(roomTypeRepository.findByIdAndDeletedAtIsNull(roomTypeId)).thenReturn(Optional.of(room(roomTypeId)));
		PromotionCode promo = fixedPromo("SAVE400", new BigDecimal("400"));
		promo.setMinPurchaseAmount(new BigDecimal("5000.00"));
		when(promotionCodeRepository.findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull("SAVE400")).thenReturn(Optional.of(promo));

		var response = service.preview("SAVE400", roomTypeId, new BigDecimal("2500"));

		assertEquals(PromotionPreviewStatus.BELOW_MINIMUM, response.status());
		assertEquals(new BigDecimal("5000.00"), response.minPurchaseAmount());
	}

	private RoomType room(UUID id) {
		RoomType type = new RoomType();
		type.setId(id);
		type.setName("Deluxe");
		return type;
	}

	private PromotionCode fixedPromo(String code, BigDecimal amountOff) {
		PromotionCode promo = new PromotionCode();
		promo.setCode(code);
		promo.setDiscountType(DiscountType.FIXED);
		promo.setAmountOff(amountOff);
		promo.setMinPurchaseAmount(BigDecimal.ZERO);
		promo.setActive(true);
		return promo;
	}

	private PromotionCodeRequest request(
			String code,
			DiscountType type,
			BigDecimal amountOff,
			BigDecimal percentOff,
			BigDecimal minPurchase,
			List<UUID> roomTypeIds) {
		return new PromotionCodeRequest(code, type, amountOff, percentOff, minPurchase, roomTypeIds);
	}
}
