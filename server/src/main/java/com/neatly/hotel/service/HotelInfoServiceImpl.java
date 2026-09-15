package com.neatly.hotel.service;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.neatly.hotel.dto.HotelInfoResponse;
import com.neatly.hotel.dto.UpdateHotelInfoRequest;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.exception.ResourceNotFoundException;
import com.neatly.hotel.model.HotelInfo;
import com.neatly.hotel.repository.HotelInfoRepository;

@Service
@Transactional
public class HotelInfoServiceImpl implements HotelInfoService {

	private static final Logger log = LoggerFactory.getLogger(HotelInfoServiceImpl.class);

	private static final long LOGO_MAX_BYTES = 2 * 1024 * 1024;
	private static final Map<String, String> LOGO_EXTENSIONS = ImageFiles.EXTENSIONS;

	private final HotelInfoRepository hotelInfoRepository;
	private final StorageService storageService;

	public HotelInfoServiceImpl(HotelInfoRepository hotelInfoRepository, StorageService storageService) {
		this.hotelInfoRepository = hotelInfoRepository;
		this.storageService = storageService;
	}

	@Override
	@Transactional(readOnly = true)
	public HotelInfoResponse get() {
		return HotelInfoResponse.from(find());
	}

	@Override
	public HotelInfoResponse update(UpdateHotelInfoRequest request) {
		HotelInfo hotelInfo = find();
		hotelInfo.setName(request.name().trim());
		hotelInfo.setDescription(request.description().trim());
		return HotelInfoResponse.from(hotelInfoRepository.saveAndFlush(hotelInfo));
	}

	/**
	 * Upload the new object, save its URL, then delete the old object.
	 * Runs without a surrounding transaction so the save is committed before the old logo is deleted.
	 */
	@Override
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public HotelInfoResponse replaceLogo(MultipartFile file) {
		byte[] content = readValidLogo(file);
		HotelInfo hotelInfo = find();
		String oldUrl = hotelInfo.getLogoUrl();

		String contentType = file.getContentType();
		String newUrl = storageService.upload(
				"logo/" + UUID.randomUUID() + "." + LOGO_EXTENSIONS.get(contentType),
				content,
				contentType);

		HotelInfo saved;
		try {
			hotelInfo.setLogoUrl(newUrl);
			saved = hotelInfoRepository.saveAndFlush(hotelInfo);
		} catch (RuntimeException ex) {
			deleteQuietly(newUrl);
			throw ex;
		}

		deleteQuietly(oldUrl);
		return HotelInfoResponse.from(saved);
	}

	private HotelInfo find() {
		return hotelInfoRepository.findFirstByOrderByCreatedAtAsc()
				.orElseThrow(() -> new ResourceNotFoundException("Hotel information not found"));
	}

	private byte[] readValidLogo(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ApiException("Logo file is required", HttpStatus.BAD_REQUEST);
		}
		if (file.getSize() > LOGO_MAX_BYTES) {
			throw new ApiException("Logo must be 2 MB or smaller", HttpStatus.CONTENT_TOO_LARGE);
		}
		byte[] content;
		try {
			content = file.getBytes();
		} catch (IOException ex) {
			throw new ApiException("Could not read logo file", HttpStatus.BAD_REQUEST);
		}
		String contentType = file.getContentType();
		if (contentType == null || !LOGO_EXTENSIONS.containsKey(contentType) || !ImageFiles.hasSignature(contentType, content)) {
			throw new ApiException("Logo must be a PNG, JPEG or WEBP image", HttpStatus.BAD_REQUEST);
		}
		return content;
	}

	private void deleteQuietly(String url) {
		try {
			storageService.deleteByPublicUrl(url);
		} catch (RuntimeException ex) {
			log.warn("Could not delete storage object {}: {}", url, ex.getMessage());
		}
	}
}
