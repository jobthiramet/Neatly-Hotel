package com.neatly.hotel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import com.neatly.hotel.dto.CreateProfileRequest;
import com.neatly.hotel.dto.ProfileResponse;
import com.neatly.hotel.dto.ProfileUpdateResponse;
import com.neatly.hotel.dto.UpdateProfileRequest;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.model.Profile;
import com.neatly.hotel.repository.ProfileRepository;

class ProfileServiceImplTest {

	private final ProfileRepository repository = mock(ProfileRepository.class);
	private final ClerkUserMirror clerkUserMirror = mock(ClerkUserMirror.class);
	private final ProfileServiceImpl service = new ProfileServiceImpl(repository, clerkUserMirror);

	private static UpdateProfileRequest update(String phone) {
		return new UpdateProfileRequest(" Kate ", " Cho ", phone, LocalDate.of(1998, 3, 12), " Thailand ", null);
	}

	@Test
	void firstLoadReturnsAnEmptyProfileWithoutSavingIt() {
		when(repository.findById("user_new")).thenReturn(Optional.empty());

		ProfileResponse response = service.findOrDefault("user_new");

		assertEquals("user_new", response.clerkUserId());
		assertNull(response.firstName());
		verify(repository, never()).save(any(Profile.class));
	}

	@Test
	void updateCreatesTheProfileTrimsTextAndNormalisesThePhoneNumber() {
		when(repository.findById("user_abc123")).thenReturn(Optional.empty());
		when(repository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(clerkUserMirror.mirrorName(any(), any(), any())).thenReturn(true);

		ProfileUpdateResponse response = service.update("user_abc123", update("088 888 8888"));

		ArgumentCaptor<Profile> saved = ArgumentCaptor.forClass(Profile.class);
		verify(repository).save(saved.capture());
		assertEquals("user_abc123", saved.getValue().getClerkUserId());
		assertEquals("Kate", saved.getValue().getFirstName());
		assertEquals("Thailand", saved.getValue().getCountry());
		// Figma shows a local number; the column stores E.164
		assertEquals("+66888888888", saved.getValue().getPhoneNumber());
		assertEquals(Boolean.TRUE, response.clerkSynced());
		verify(clerkUserMirror).mirrorName("user_abc123", "Kate", "Cho");
	}

	@Test
	void e164NumbersAreKeptAsTheyAre() {
		when(repository.findById("user_abc123")).thenReturn(Optional.empty());
		when(repository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.update("user_abc123", update("+66 81 234 5678"));

		ArgumentCaptor<Profile> saved = ArgumentCaptor.forClass(Profile.class);
		verify(repository).save(saved.capture());
		assertEquals("+66812345678", saved.getValue().getPhoneNumber());
	}

	@Test
	void aFailedClerkMirrorKeepsTheSaveAndReportsIt() {
		when(repository.findById("user_abc123")).thenReturn(Optional.empty());
		when(repository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(clerkUserMirror.mirrorName(any(), any(), any())).thenReturn(false);

		ProfileUpdateResponse response = service.update("user_abc123", update("+66812345678"));

		assertEquals("Kate", response.profile().firstName());
		assertEquals(Boolean.FALSE, response.clerkSynced());
		verify(repository).save(any(Profile.class));
	}

	@Test
	void mirroringIsSkippedWhenClerkIsNotConfigured() {
		when(repository.findById("user_abc123")).thenReturn(Optional.empty());
		when(repository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(clerkUserMirror.mirrorName(any(), any(), any())).thenReturn(null);

		assertNull(service.update("user_abc123", update("+66812345678")).clerkSynced());
	}

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
