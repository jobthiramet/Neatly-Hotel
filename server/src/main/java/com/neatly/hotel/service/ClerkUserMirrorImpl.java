package com.neatly.hotel.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Clerk Backend API. Only the name is mirrored: Clerk phone numbers and emails are separate
 * resources that need their own verification flows, and Clerk owns those.
 */
@Service
public class ClerkUserMirrorImpl implements ClerkUserMirror {

	private static final Logger log = LoggerFactory.getLogger(ClerkUserMirrorImpl.class);

	private final RestClient restClient;
	private final String secretKey;

	public ClerkUserMirrorImpl(
			@Value("${clerk.secret-key:}") String secretKey,
			@Value("${clerk.api-url:https://api.clerk.com/v1}") String apiUrl) {
		this.restClient = RestClient.create(apiUrl);
		this.secretKey = secretKey;
	}

	@Override
	public Boolean mirrorName(String clerkUserId, String firstName, String lastName) {
		if (secretKey.isBlank()) {
			return null;
		}
		try {
			restClient.patch()
					.uri("/users/{id}", clerkUserId)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("first_name", firstName, "last_name", lastName))
					.retrieve()
					.toBodilessEntity();
			return true;
		} catch (Exception exception) {
			// The profile is already saved; the caller reports the mirror failure instead of failing the save.
			log.warn("Could not mirror profile {} to Clerk: {}", clerkUserId, exception.getMessage());
			return false;
		}
	}
}
