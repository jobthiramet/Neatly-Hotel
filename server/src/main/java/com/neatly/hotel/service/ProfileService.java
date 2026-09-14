package com.neatly.hotel.service;

import com.neatly.hotel.dto.CreateProfileRequest;
import com.neatly.hotel.dto.ProfileResponse;

public interface ProfileService {

	ProfileResponse findByClerkUserId(String clerkUserId);

	ProfileResponse create(String clerkUserId, CreateProfileRequest request);
}
