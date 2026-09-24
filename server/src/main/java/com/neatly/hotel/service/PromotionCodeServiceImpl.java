package com.neatly.hotel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neatly.hotel.dto.PromotionCodePreviewResponse;
import com.neatly.hotel.dto.PromotionCodeRequest;
import com.neatly.hotel.dto.PromotionCodeResponse;
import com.neatly.hotel.dto.PromotionPreviewStatus;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.exception.ResourceNotFoundException;
import com.neatly.hotel.model.DiscountType;
import com.neatly.hotel.model.PromotionCode;
import com.neatly.hotel.model.RoomType;
import com.neatly.hotel.repository.PromotionCodeRepository;
import com.neatly.hotel.repository.RoomTypeRepository;

@Service
@Transactional
public class PromotionCodeServiceImpl implements PromotionCodeService {

	private final PromotionCodeRepository promotionCodeRepository;
	private final RoomTypeRepository roomTypeRepository;

	public PromotionCodeServiceImpl(
			PromotionCodeRepository promotionCodeRepository,
			RoomTypeRepository roomTypeRepository) {
		this.promotionCodeRepository = promotionCodeRepository;
		this.roomTypeRepository = roomTypeRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<PromotionCodeResponse> findAll() {
		return promotionCodeRepository.findByDeletedAtIsNullOrderByCodeAsc().stream()
				.map(PromotionCodeResponse::from)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public PromotionCodeResponse findById(UUID id) {
		return PromotionCodeResponse.from(requirePromo(id));
	}

	@Override
	public PromotionCodeResponse create(PromotionCodeRequest request) {
		String code = normalizeCode(request.code());
		if (promotionCodeRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
			throw new ApiException("Promo code already exists: " + code, HttpStatus.CONFLICT);
		}
		PromotionCode promo = new PromotionCode();
		apply(promo, request, code);
		return PromotionCodeResponse.from(promotionCodeRepository.save(promo));
	}

	@Override
	public PromotionCodeResponse update(UUID id, PromotionCodeRequest request) {
		PromotionCode promo = requirePromo(id);
		String code = normalizeCode(request.code());
		if (promotionCodeRepository.existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(code, id)) {
			throw new ApiException("Promo code already exists: " + code, HttpStatus.CONFLICT);
		}
		apply(promo, request, code);
		return PromotionCodeResponse.from(promotionCodeRepository.save(promo));
	}

	@Override
	public void delete(UUID id) {
		PromotionCode promo = requirePromo(id);
		promo.setDeletedAt(Instant.now());
		promotionCodeRepository.save(promo);
	}

	@Override
	@Transactional(readOnly = true)
	public PromotionCodePreviewResponse preview(String code, UUID roomTypeId, BigDecimal purchase) {
		if (purchase == null || purchase.signum() < 0) {
			throw new ApiException("Purchase amount must be zero or more", HttpStatus.BAD_REQUEST);
		}
		String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
		if (!normalized.matches("^[A-Z0-9-]{1,40}$")) {
			return PromotionCodePreviewResponse.notFound();
		}
		RoomType roomType = roomTypeRepository.findByIdAndDeletedAtIsNull(roomTypeId)
				.orElseThrow(() -> new ResourceNotFoundException("Room type not found"));
		PromotionCode promo = promotionCodeRepository
				.findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(normalized)
				.orElse(null);
		if (promo == null) {
			return PromotionCodePreviewResponse.notFound();
		}
		if (!appliesToRoom(promo, roomType)) {
			return new PromotionCodePreviewResponse(PromotionPreviewStatus.ROOM_NOT_ELIGIBLE, null, null);
		}
		BigDecimal minimum = promo.getMinPurchaseAmount() == null ? BigDecimal.ZERO : promo.getMinPurchaseAmount();
		if (purchase.compareTo(minimum) < 0) {
			return new PromotionCodePreviewResponse(
					PromotionPreviewStatus.BELOW_MINIMUM,
					null,
					minimum.setScale(2, RoundingMode.HALF_UP));
		}
		BigDecimal discount = discountAmount(promo, purchase);
		if (discount == null || discount.signum() <= 0) {
			return PromotionCodePreviewResponse.notFound();
		}
		return new PromotionCodePreviewResponse(PromotionPreviewStatus.APPLIED, discount, null);
	}

	private void apply(PromotionCode promo, PromotionCodeRequest request, String code) {
		promo.setCode(code);
		promo.setDiscountType(request.discountType());
		promo.setMinPurchaseAmount(request.minPurchaseAmount().setScale(2, RoundingMode.HALF_UP));
		promo.setActive(true);
		if (request.discountType() == DiscountType.FIXED) {
			if (request.amountOff() == null) {
				throw new ApiException("Fixed discount amount is required", HttpStatus.BAD_REQUEST);
			}
			promo.setAmountOff(request.amountOff().setScale(2, RoundingMode.HALF_UP));
			promo.setPercentOff(null);
		}
		else {
			if (request.percentOff() == null) {
				throw new ApiException("Percent discount is required", HttpStatus.BAD_REQUEST);
			}
			promo.setPercentOff(request.percentOff().setScale(2, RoundingMode.HALF_UP));
			promo.setAmountOff(null);
		}
		promo.getRoomTypes().clear();
		promo.getRoomTypes().addAll(resolveRoomTypes(request.roomTypeIds()));
	}

	private List<RoomType> resolveRoomTypes(List<UUID> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		List<UUID> distinct = ids.stream().distinct().toList();
		Map<UUID, RoomType> found = new HashMap<>();
		for (RoomType type : roomTypeRepository.findAllById(distinct)) {
			if (type.getDeletedAt() == null) {
				found.put(type.getId(), type);
			}
		}
		if (found.size() != distinct.size()) {
			throw new ApiException("One or more room types were not found", HttpStatus.BAD_REQUEST);
		}
		return distinct.stream().map(found::get).toList();
	}

	private PromotionCode requirePromo(UUID id) {
		return promotionCodeRepository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new ResourceNotFoundException("Promo code not found: " + id));
	}

	private static String normalizeCode(String code) {
		return code.trim().toUpperCase(Locale.ROOT);
	}

	/** Same rule as checkout: an empty room-type list applies to every type. Deleted types are ignored. */
	private boolean appliesToRoom(PromotionCode promo, RoomType roomType) {
		if (promo.getRoomTypes() == null || promo.getRoomTypes().isEmpty()) {
			return true;
		}
		return promo.getRoomTypes().stream()
				.filter(type -> type.getDeletedAt() == null)
				.anyMatch(type -> roomType.getId().equals(type.getId()));
	}

	/** Positive discount, or null when the code has no amount. Purchase must already meet the minimum. */
	private BigDecimal discountAmount(PromotionCode promo, BigDecimal purchase) {
		if (promo.getDiscountType() == DiscountType.PERCENT) {
			if (promo.getPercentOff() == null) {
				return null;
			}
			return purchase.multiply(promo.getPercentOff()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
		}
		if (promo.getAmountOff() == null) {
			return null;
		}
		return promo.getAmountOff().setScale(2, RoundingMode.HALF_UP);
	}
}
