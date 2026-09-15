package com.neatly.hotel.service;

import org.springframework.web.multipart.MultipartFile;

public interface ProfilePictureService {

	String upload(String clerkUserId, MultipartFile file);
}
