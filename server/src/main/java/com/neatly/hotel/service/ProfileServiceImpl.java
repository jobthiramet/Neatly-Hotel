package com.neatly.hotel.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neatly.hotel.dto.CreateProfileRequest;
import com.neatly.hotel.dto.ProfileResponse;
import com.neatly.hotel.dto.ProfileUpdateResponse;
import com.neatly.hotel.dto.UpdateProfileRequest;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.exception.ResourceNotFoundException;
import com.neatly.hotel.model.Profile;
import com.neatly.hotel.repository.ProfileRepository;

@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

	private final ProfileRepository profileRepository;
	private final ClerkUserMirror clerkUserMirror;

	public ProfileServiceImpl(ProfileRepository profileRepository, ClerkUserMirror clerkUserMirror) {
		this.profileRepository = profileRepository;
		this.clerkUserMirror = clerkUserMirror;
	}

	@Override
	@Transactional(readOnly = true)
	public ProfileResponse findOrDefault(String clerkUserId) {
		// An empty row would break the not-null columns (date of birth has no sensible default),
		// so the blank profile is not persisted until the guest saves it.
		return profileRepository.findById(clerkUserId)
				.map(ProfileResponse::from)
				.orElseGet(() -> ProfileResponse.from(blankProfile(clerkUserId)));
	}

	@Override
	public ProfileUpdateResponse update(String clerkUserId, UpdateProfileRequest request) {
		Profile profile = profileRepository.findById(clerkUserId).orElseGet(() -> blankProfile(clerkUserId));
		profile.setFirstName(request.firstName().trim());
		profile.setLastName(request.lastName().trim());
		profile.setPhoneNumber(toE164(request.phoneNumber()));
		profile.setDateOfBirth(request.dateOfBirth());
		profile.setCountry(request.country().trim());
		profile.setProfilePicture(request.profilePicture());
		ProfileResponse saved = ProfileResponse.from(profileRepository.save(profile));

		Boolean synced = clerkUserMirror.mirrorName(clerkUserId, saved.firstName(), saved.lastName());
		return new ProfileUpdateResponse(saved, synced);
	}

	private static Profile blankProfile(String clerkUserId) {
		Profile profile = new Profile();
		profile.setClerkUserId(clerkUserId);
		return profile;
	}

	/**
	 * Figma shows local numbers ("088 888 8888"); the column stores E.164.
	 * ponytail: assumes a Thai local number, matching the hotel. Take the dialling code from
	 * the selected country if the hotel ever sells abroad.
	 */
	static String toE164(String phoneNumber) {
		String digits = phoneNumber.replaceAll("[^+0-9]", "");
		if (digits.startsWith("+")) {
			return digits;
		}
		return digits.startsWith("0") ? "+66" + digits.substring(1) : "+" + digits;
	}

	@Override
	@Transactional(readOnly = true)
	public ProfileResponse findByClerkUserId(String clerkUserId) {
		return profileRepository.findById(clerkUserId)
				.map(ProfileResponse::from)
				.orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + clerkUserId));
	}

	@Override
	public ProfileResponse create(String clerkUserId, CreateProfileRequest request) {
		if (profileRepository.existsById(clerkUserId)) {
			throw new ApiException("Profile already exists: " + clerkUserId, HttpStatus.CONFLICT);
		}

		Profile profile = new Profile();
		profile.setClerkUserId(clerkUserId);
		profile.setFirstName(request.firstName().trim());
		profile.setLastName(request.lastName().trim());
		profile.setPhoneNumber(request.phoneNumber());
		profile.setDateOfBirth(request.dateOfBirth());
		profile.setCountry(request.country().trim());
		profile.setProfilePicture(request.profilePicture());
		return ProfileResponse.from(profileRepository.save(profile));
	}
}
