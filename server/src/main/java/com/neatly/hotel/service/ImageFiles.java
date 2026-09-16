package com.neatly.hotel.service;

import java.util.Map;

/** Allowed upload image types (no SVG: it can carry scripts) and their magic-byte check. */
final class ImageFiles {

	static final Map<String, String> EXTENSIONS = Map.of(
			"image/png", "png",
			"image/jpeg", "jpg",
			"image/webp", "webp");

	private ImageFiles() {
	}

	/** The declared content type comes from the client, so also check the file's magic bytes. */
	static boolean hasSignature(String contentType, byte[] content) {
		if (contentType == null) {
			return false;
		}
		return switch (contentType) {
			case "image/png" -> bytesAt(content, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
			case "image/jpeg" -> bytesAt(content, 0, 0xFF, 0xD8, 0xFF);
			case "image/webp" -> bytesAt(content, 0, 0x52, 0x49, 0x46, 0x46) && bytesAt(content, 8, 0x57, 0x45, 0x42, 0x50);
			default -> false;
		};
	}

	private static boolean bytesAt(byte[] content, int offset, int... expected) {
		if (content.length < offset + expected.length) {
			return false;
		}
		for (int i = 0; i < expected.length; i++) {
			if ((content[offset + i] & 0xFF) != expected[i]) {
				return false;
			}
		}
		return true;
	}
}
