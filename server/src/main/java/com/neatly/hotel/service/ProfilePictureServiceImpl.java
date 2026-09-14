package com.neatly.hotel.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.neatly.hotel.exception.ApiException;

@Service
public class ProfilePictureServiceImpl implements ProfilePictureService {

	private static final long MAX_BYTES = 5 * 1024 * 1024;
	private static final Map<String, String> EXTENSIONS = Map.of(
			"image/png", "png",
			"image/jpeg", "jpg",
			"image/webp", "webp");

	private final StorageService storageService;
	private final String bucket;

	public ProfilePictureServiceImpl(
			StorageService storageService,
			@Value("${supabase.storage.profile-bucket}") String bucket) {
		this.storageService = storageService;
		this.bucket = bucket;
	}

	@Override
	public String upload(String clerkUserId, MultipartFile file) {
		if (clerkUserId == null || !clerkUserId.matches("user_[A-Za-z0-9]+")) {
			throw new ApiException("Invalid Clerk user ID", HttpStatus.UNAUTHORIZED);
		}
		if (file == null || file.isEmpty()) {
			throw new ApiException("Profile picture is required", HttpStatus.BAD_REQUEST);
		}
		if (file.getSize() > MAX_BYTES) {
			throw new ApiException("Profile picture must be 5 MB or smaller", HttpStatus.CONTENT_TOO_LARGE);
		}

		String contentType = file.getContentType();
		String extension = EXTENSIONS.get(contentType);
		if (extension == null) {
			throw new ApiException("Profile picture must be a PNG, JPEG or WEBP image", HttpStatus.BAD_REQUEST);
		}
		byte[] content = read(file);
		if (!matchesSignature(content, contentType)) {
			throw new ApiException("Profile picture content does not match its type", HttpStatus.BAD_REQUEST);
		}

		String path = "users/" + clerkUserId + "/avatar." + extension;
		storageService.uploadOrReplace(bucket, path, content, contentType);
		return path;
	}

	private byte[] read(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException ex) {
			throw new ApiException("Could not read profile picture", HttpStatus.BAD_REQUEST);
		}
	}

	private boolean matchesSignature(byte[] content, String contentType) {
		return switch (contentType) {
			case "image/png" -> startsWith(content, new int[] { 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A });
			case "image/jpeg" -> startsWith(content, new int[] { 0xFF, 0xD8, 0xFF });
			case "image/webp" -> startsWith(content, new int[] { 'R', 'I', 'F', 'F' })
					&& content.length >= 12
					&& content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P';
			default -> false;
		};
	}

	private boolean startsWith(byte[] content, int[] signature) {
		if (content.length < signature.length) return false;
		for (int i = 0; i < signature.length; i++) {
			if ((content[i] & 0xFF) != signature[i]) return false;
		}
		return true;
	}
}
