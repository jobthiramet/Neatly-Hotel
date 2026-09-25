package com.neatly.hotel.service;

import com.neatly.hotel.dto.CreateProfileRequest;
import com.neatly.hotel.dto.ProfileResponse;
import com.neatly.hotel.dto.ProfileUpdateResponse;
import com.neatly.hotel.dto.UpdateProfileRequest;

public interface ProfileService {

	ProfileResponse findByClerkUserId(String clerkUserId);

	ProfileResponse create(String clerkUserId, CreateProfileRequest request);

	/** The saved profile, or an empty one to fill in when the guest has no row yet. */
	ProfileResponse findOrDefault(String clerkUserId);

	/** Saves the profile (creating it on first save) and mirrors the name to Clerk. */
	ProfileUpdateResponse update(String clerkUserId, UpdateProfileRequest request);
}
