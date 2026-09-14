package com.neatly.hotel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.neatly.hotel.dto.HotelInfoResponse;
import com.neatly.hotel.dto.UpdateHotelInfoRequest;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.model.HotelInfo;
import com.neatly.hotel.repository.HotelInfoRepository;

class HotelInfoServiceImplTest {

	private static final String OLD_URL = "https://x.supabase.co/storage/v1/object/public/hotel-assets/logo/old.png";
	private static final String NEW_URL = "https://x.supabase.co/storage/v1/object/public/hotel-assets/logo/new.png";
	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0 };

	private final HotelInfoRepository repository = mock(HotelInfoRepository.class);
	private final StorageService storage = mock(StorageService.class);
	private final HotelInfoServiceImpl service = new HotelInfoServiceImpl(repository, storage);
	private HotelInfo hotelInfo;

	@BeforeEach
	void setUp() {
		hotelInfo = new HotelInfo();
		hotelInfo.setName("Old");
		hotelInfo.setDescription("Old description");
		hotelInfo.setLogoUrl(OLD_URL);
		when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(hotelInfo));
		when(repository.saveAndFlush(any(HotelInfo.class))).thenAnswer(call -> call.getArgument(0));
	}

	@Test
	void updateTrimsAndSavesNameAndDescription() {
		HotelInfoResponse response = service.update(new UpdateHotelInfoRequest("  Neatly  ", " Nice hotel "));

		assertEquals("Neatly", response.name());
		assertEquals("Nice hotel", response.description());
		verify(repository).saveAndFlush(hotelInfo);
	}

	@Test
	void replaceLogoUploadsThenSavesThenDeletesOld() {
		when(storage.upload(startsWith("logo/"), any(), eq("image/png"))).thenReturn(NEW_URL);

		HotelInfoResponse response = service.replaceLogo(png());

		assertEquals(NEW_URL, response.logoUrl());
		InOrder order = inOrder(storage, repository);
		order.verify(storage).upload(startsWith("logo/"), any(), eq("image/png"));
		order.verify(repository).saveAndFlush(hotelInfo);
		order.verify(storage).deleteByPublicUrl(OLD_URL);
	}

	@Test
	void replaceLogoKeepsDatabaseUnchangedWhenUploadFails() {
		when(storage.upload(anyString(), any(), anyString()))
				.thenThrow(new ApiException("Failed to upload file to storage", HttpStatus.BAD_GATEWAY));

		ApiException ex = assertThrows(ApiException.class, () -> service.replaceLogo(png()));

		assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
		assertEquals(OLD_URL, hotelInfo.getLogoUrl());
		verify(repository, never()).saveAndFlush(any());
		verify(storage, never()).deleteByPublicUrl(any());
	}

	@Test
	void replaceLogoSucceedsWhenOldObjectDeleteFails() {
		when(storage.upload(anyString(), any(), anyString())).thenReturn(NEW_URL);
		doThrow(new ApiException("boom", HttpStatus.BAD_GATEWAY)).when(storage).deleteByPublicUrl(OLD_URL);

		assertEquals(NEW_URL, service.replaceLogo(png()).logoUrl());
	}

	@Test
	void replaceLogoRejectsFileWhoseBytesDoNotMatchType() {
		MockMultipartFile svgAsPng = new MockMultipartFile("file", "logo.png", "image/png", "<svg/>".getBytes());

		ApiException ex = assertThrows(ApiException.class, () -> service.replaceLogo(svgAsPng));

		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
		verify(storage, never()).upload(anyString(), any(), anyString());
	}

	@Test
	void replaceLogoRejectsFileOverTwoMegabytes() {
		byte[] big = new byte[2 * 1024 * 1024 + 1];
		System.arraycopy(PNG, 0, big, 0, PNG.length);

		ApiException ex = assertThrows(ApiException.class,
				() -> service.replaceLogo(new MockMultipartFile("file", "logo.png", "image/png", big)));

		assertEquals(HttpStatus.CONTENT_TOO_LARGE, ex.getStatus());
	}

	private static MockMultipartFile png() {
		return new MockMultipartFile("file", "whatever.png", "image/png", PNG);
	}
}
