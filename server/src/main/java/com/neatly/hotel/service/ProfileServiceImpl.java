package com.neatly.hotel.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neatly.hotel.dto.CreateProfileRequest;
import com.neatly.hotel.dto.ProfileResponse;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.exception.ResourceNotFoundException;
import com.neatly.hotel.model.Profile;
import com.neatly.hotel.repository.ProfileRepository;

@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

	private final ProfileRepository profileRepository;

	public ProfileServiceImpl(ProfileRepository profileRepository) {
		this.profileRepository = profileRepository;
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
