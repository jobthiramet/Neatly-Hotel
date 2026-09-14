package com.neatly.hotel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import com.neatly.hotel.dto.CreateProfileRequest;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.model.Profile;
import com.neatly.hotel.repository.ProfileRepository;

class ProfileServiceImplTest {

	private final ProfileRepository repository = mock(ProfileRepository.class);
	private final ProfileServiceImpl service = new ProfileServiceImpl(repository);

	@Test
	void createsUserProfileWithTrimmedText() {
		CreateProfileRequest request = new CreateProfileRequest(
				" Jane ",
				" Doe ",
				"+66812345678",
				LocalDate.of(1990, 1, 1),
				" Thailand ",
				null);
		when(repository.save(org.mockito.ArgumentMatchers.any(Profile.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.create("user_abc123", request);

		ArgumentCaptor<Profile> profile = ArgumentCaptor.forClass(Profile.class);
		verify(repository).save(profile.capture());
		assertEquals("Jane", profile.getValue().getFirstName());
		assertEquals("Doe", profile.getValue().getLastName());
		assertEquals("Thailand", profile.getValue().getCountry());
		assertEquals("user", profile.getValue().getRole());
	}

	@Test
	void rejectsDuplicateClerkUserId() {
		when(repository.existsById("user_abc123")).thenReturn(true);
		CreateProfileRequest request = new CreateProfileRequest(
				"Jane",
				"Doe",
				"+66812345678",
				LocalDate.of(1990, 1, 1),
				"Thailand",
				null);

		ApiException exception = assertThrows(
				ApiException.class,
				() -> service.create("user_abc123", request));

		assertEquals(HttpStatus.CONFLICT, exception.getStatus());
	}
}
