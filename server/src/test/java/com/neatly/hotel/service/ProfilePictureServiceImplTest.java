package com.neatly.hotel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.neatly.hotel.exception.ApiException;

class ProfilePictureServiceImplTest {

	private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0 };

	private final StorageService storage = mock(StorageService.class);
	private final ProfilePictureServiceImpl service = new ProfilePictureServiceImpl(storage, "profile-pictures");

	@Test
	void uploadsToPathOwnedByAuthenticatedClerkUser() {
		MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG);

		String path = service.upload("user_abc123", file);

		assertEquals("users/user_abc123/avatar.png", path);
		verify(storage).uploadOrReplace("profile-pictures", path, PNG, "image/png");
	}

	@Test
	void rejectsContentThatDoesNotMatchDeclaredType() {
		MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "not png".getBytes());

		ApiException exception = assertThrows(ApiException.class, () -> service.upload("user_abc123", file));

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		verify(storage, never()).uploadOrReplace(anyString(), anyString(), any(), anyString());
	}
}
