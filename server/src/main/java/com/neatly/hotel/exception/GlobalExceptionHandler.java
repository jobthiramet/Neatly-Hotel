package com.neatly.hotel.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
		return build(ex.getStatus(), ex.getMessage(), request.getRequestURI(), List.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(
			MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<String> details = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.toList();
		return build(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), details);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorResponse> handleUploadTooLarge(
			MaxUploadSizeExceededException ex,
			HttpServletRequest request) {
		return build(HttpStatus.CONTENT_TOO_LARGE, "File must be 2 MB or smaller", request.getRequestURI(), List.of());
	}

	@ExceptionHandler({ MissingServletRequestPartException.class, MultipartException.class })
	public ResponseEntity<ErrorResponse> handleBadMultipart(Exception ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Invalid multipart request", request.getRequestURI(),
				List.of(ex.getMessage() == null ? "unknown" : ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		return build(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Unexpected server error",
				request.getRequestURI(),
				List.of(ex.getMessage() == null ? "unknown" : ex.getMessage()));
	}

	private ResponseEntity<ErrorResponse> build(
			HttpStatus status,
			String message,
			String path,
			List<String> details) {
		ErrorResponse body = new ErrorResponse(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				path,
				details);
		return ResponseEntity.status(status).body(body);
	}
}
