package com.neatly.hotel.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.neatly.hotel.dto.PageResponse;
import com.neatly.hotel.dto.ReorderRoomImagesRequest;
import com.neatly.hotel.dto.RoomRequest;
import com.neatly.hotel.dto.RoomResponse;
import com.neatly.hotel.dto.RoomSummaryResponse;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.exception.ResourceNotFoundException;
import com.neatly.hotel.model.Amenity;
import com.neatly.hotel.model.RoomType;
import com.neatly.hotel.model.RoomTypeImage;
import com.neatly.hotel.repository.AmenityRepository;
import com.neatly.hotel.repository.RoomTypeImageRepository;
import com.neatly.hotel.repository.RoomTypeRepository;

@Service
@Transactional
public class RoomTypeServiceImpl implements RoomTypeService {

	private static final Logger log = LoggerFactory.getLogger(RoomTypeServiceImpl.class);

	static final int MAX_PAGE_SIZE = 50;
	static final int MIN_GALLERY = 4;
	static final int MAX_GALLERY = 12;
	private static final long IMAGE_MAX_BYTES = 5 * 1024 * 1024;
	private static final Map<String, String> IMAGE_EXTENSIONS = ImageFiles.EXTENSIONS;

	private final RoomTypeRepository roomRepository;
	private final RoomTypeImageRepository roomImageRepository;
	private final AmenityRepository amenityRepository;
	private final StorageService storageService;
	private final String bucket;

	public RoomTypeServiceImpl(
			RoomTypeRepository roomRepository,
			RoomTypeImageRepository roomImageRepository,
			AmenityRepository amenityRepository,
			StorageService storageService,
			@Value("${supabase.storage.room-bucket}") String bucket) {
		this.roomRepository = roomRepository;
		this.roomImageRepository = roomImageRepository;
		this.amenityRepository = amenityRepository;
		this.storageService = storageService;
		this.bucket = bucket;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<RoomSummaryResponse> list(String search, int page, int size) {
		PageRequest pageable = PageRequest.of(
				Math.max(page, 0),
				Math.clamp(size, 1, MAX_PAGE_SIZE),
				Sort.by(Sort.Direction.DESC, "createdAt"));
		String term = search == null ? "" : search.trim().toLowerCase();
		return PageResponse.from(roomRepository.searchActive(term, pageable), RoomSummaryResponse::from);
	}

	@Override
	@Transactional(readOnly = true)
	public RoomResponse findById(UUID id) {
		return RoomResponse.from(findActive(id));
	}

	/**
	 * Inserts the room (its id is part of the storage path), uploads every image, then saves the images.
	 * Any failure rolls the insert back and deletes the objects uploaded so far, so no half-created room remains.
	 */
	@Override
	public RoomResponse create(RoomRequest request, MultipartFile mainImage, List<MultipartFile> gallery) {
		byte[] mainContent = readValidImage(mainImage, "Main image");
		List<MultipartFile> galleryFiles = gallery == null ? List.of() : gallery;
		if (galleryFiles.size() < MIN_GALLERY || galleryFiles.size() > MAX_GALLERY) {
			throw new ApiException(
					"Image gallery needs " + MIN_GALLERY + " to " + MAX_GALLERY + " images",
					HttpStatus.BAD_REQUEST);
		}
		List<byte[]> galleryContents = galleryFiles.stream().map(file -> readValidImage(file, "Gallery image")).toList();
		if (roomRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(request.name().trim())) {
			throw duplicateName();
		}

		RoomType room = new RoomType();
		apply(room, request);
		room = roomRepository.saveAndFlush(room);

		List<String> uploadedPaths = new ArrayList<>();
		try {
			room.getImages().add(upload(room, mainImage.getContentType(), mainContent, true, 0, uploadedPaths));
			for (int i = 0; i < galleryFiles.size(); i++) {
				room.getImages().add(
						upload(room, galleryFiles.get(i).getContentType(), galleryContents.get(i), false, i + 1, uploadedPaths));
			}
			return RoomResponse.from(roomRepository.saveAndFlush(room));
		} catch (RuntimeException ex) {
			uploadedPaths.forEach(this::deleteQuietly);
			throw ex;
		}
	}

	@Override
	public RoomResponse update(UUID id, RoomRequest request) {
		RoomType room = findActive(id);
		if (roomRepository.existsByNameIgnoreCaseAndDeletedAtIsNullAndIdNot(request.name().trim(), id)) {
			throw duplicateName();
		}
		apply(room, request);
		return RoomResponse.from(roomRepository.saveAndFlush(room));
	}

	/** Soft delete. Images stay in storage for booking history. */
	@Override
	public void delete(UUID id) {
		RoomType room = findActive(id);
		room.setDeletedAt(Instant.now());
		roomRepository.saveAndFlush(room);
	}

	/** Adds a gallery image, or replaces the main image and deletes the old main object after the DB update. */
	@Override
	public RoomResponse addImage(UUID id, MultipartFile file, boolean main) {
		byte[] content = readValidImage(file, main ? "Main image" : "Gallery image");
		RoomType room = findActive(id);
		List<RoomTypeImage> gallery = gallery(room);
		if (!main && gallery.size() >= MAX_GALLERY) {
			throw new ApiException("Image gallery can have at most " + MAX_GALLERY + " images", HttpStatus.BAD_REQUEST);
		}
		RoomTypeImage oldMain = main ? room.getImages().stream().filter(RoomTypeImage::getIsMain).findFirst().orElse(null) : null;
		int sortOrder = main ? 0 : gallery.stream().mapToInt(RoomTypeImage::getSortOrder).max().orElse(0) + 1;

		List<String> uploadedPaths = new ArrayList<>();
		RoomTypeImage image = upload(room, file.getContentType(), content, main, sortOrder, uploadedPaths);
		RoomType saved;
		try {
			if (oldMain != null) {
				// Delete first: one main image per room is enforced by a unique index.
				room.getImages().remove(oldMain);
				roomImageRepository.delete(oldMain);
				roomImageRepository.flush();
			}
			room.getImages().add(image);
			saved = roomRepository.saveAndFlush(room);
		} catch (RuntimeException ex) {
			uploadedPaths.forEach(this::deleteQuietly);
			throw ex;
		}
		// ponytail: runs after flush, not after commit; move to an afterCommit hook if commits start failing here.
		if (oldMain != null) {
			deleteQuietly(oldMain.getStoragePath());
		}
		return RoomResponse.from(saved);
	}

	@Override
	public RoomResponse deleteImage(UUID id, UUID imageId) {
		RoomType room = findActive(id);
		RoomTypeImage image = room.getImages().stream()
				.filter(candidate -> candidate.getId().equals(imageId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Room image not found: " + imageId));
		if (image.getIsMain()) {
			throw new ApiException("Main image is required; upload a new main image to replace it", HttpStatus.BAD_REQUEST);
		}
		if (gallery(room).size() <= MIN_GALLERY) {
			throw new ApiException("Image gallery needs at least " + MIN_GALLERY + " images", HttpStatus.BAD_REQUEST);
		}
		room.getImages().remove(image);
		roomImageRepository.delete(image);
		RoomType saved = roomRepository.saveAndFlush(room);
		deleteQuietly(image.getStoragePath());
		return RoomResponse.from(saved);
	}

	@Override
	public RoomResponse reorderImages(UUID id, ReorderRoomImagesRequest request) {
		RoomType room = findActive(id);
		List<RoomTypeImage> gallery = gallery(room);
		List<UUID> ids = request.imageIds();
		boolean sameImages = ids.size() == gallery.size()
				&& new HashSet<>(ids).equals(new HashSet<>(gallery.stream().map(RoomTypeImage::getId).toList()));
		if (!sameImages) {
			throw new ApiException("imageIds must list every gallery image of the room exactly once", HttpStatus.BAD_REQUEST);
		}
		for (RoomTypeImage image : gallery) {
			image.setSortOrder(ids.indexOf(image.getId()) + 1);
		}
		room.getImages().sort(Comparator.comparing(RoomTypeImage::getSortOrder));
		return RoomResponse.from(roomRepository.saveAndFlush(room));
	}

	private RoomType findActive(UUID id) {
		return roomRepository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
	}

	private void apply(RoomType room, RoomRequest request) {
		room.setName(request.name().trim());
		room.setBedType(request.bedType());
		room.setSizeSqm(request.sizeSqm());
		room.setCapacity(request.capacity());
		room.setPricePerNight(request.pricePerNight());
		room.setPromotionPrice(request.promotionPrice());
		room.setDescription(request.description().trim());
		room.getAmenities().clear();
		HashSet<String> seen = new HashSet<>();
		for (String name : request.amenities()) {
			String trimmed = name.trim();
			if (seen.add(trimmed.toLowerCase())) {
				room.getAmenities().add(findOrCreateAmenity(trimmed));
			}
		}
	}

	/** Amenities are shared by name (case-insensitive); unknown names are created. */
	private Amenity findOrCreateAmenity(String name) {
		return amenityRepository.findByNameIgnoreCase(name).orElseGet(() -> {
			Amenity amenity = new Amenity();
			amenity.setName(name);
			return amenityRepository.save(amenity);
		});
	}

	private static List<RoomTypeImage> gallery(RoomType room) {
		return room.getImages().stream().filter(image -> !image.getIsMain()).toList();
	}

	/** Object path is server-generated; the client filename is never used. */
	private RoomTypeImage upload(
			RoomType room, String contentType, byte[] content, boolean main, int sortOrder, List<String> uploadedPaths) {
		String path = "rooms/" + room.getId() + "/" + UUID.randomUUID() + "." + IMAGE_EXTENSIONS.get(contentType);
		String url = storageService.uploadOrReplace(bucket, path, content, contentType);
		uploadedPaths.add(path);
		RoomTypeImage image = new RoomTypeImage();
		image.setRoomType(room);
		image.setUrl(url);
		image.setStoragePath(path);
		image.setIsMain(main);
		image.setSortOrder(sortOrder);
		return image;
	}

	private byte[] readValidImage(MultipartFile file, String label) {
		if (file == null || file.isEmpty()) {
			throw new ApiException(label + " is required", HttpStatus.BAD_REQUEST);
		}
		if (file.getSize() > IMAGE_MAX_BYTES) {
			throw new ApiException(label + " must be 5 MB or smaller", HttpStatus.CONTENT_TOO_LARGE);
		}
		byte[] content;
		try {
			content = file.getBytes();
		} catch (IOException ex) {
			throw new ApiException("Could not read " + label.toLowerCase(), HttpStatus.BAD_REQUEST);
		}
		String contentType = file.getContentType();
		if (!IMAGE_EXTENSIONS.containsKey(contentType) || !ImageFiles.hasSignature(contentType, content)) {
			throw new ApiException(label + " must be a PNG, JPEG or WEBP image", HttpStatus.BAD_REQUEST);
		}
		return content;
	}

	private static ApiException duplicateName() {
		return new ApiException("A room with this room type already exists", HttpStatus.CONFLICT);
	}

	private void deleteQuietly(String path) {
		try {
			storageService.delete(bucket, path);
		} catch (RuntimeException ex) {
			log.warn("Could not delete storage object {}: {}", path, ex.getMessage());
		}
	}
}
