package com.neatly.hotel.dto;

import java.time.Instant;

public record ApiResponse<T>(
		boolean success,
		String message,
		T data,
		Instant timestamp) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, "OK", data, Instant.now());
	}

	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>(true, message, data, Instant.now());
	}

	public static ApiResponse<Void> fail(String message) {
		return new ApiResponse<>(false, message, null, Instant.now());
	}
}
