package com.neatly.hotel.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.neatly.hotel.exception.ApiException;

/** Supabase Storage REST client. The service role key stays on the server and is never logged. */
@Service
public class StorageServiceImpl implements StorageService {

	private final String supabaseUrl;
	private final String serviceRoleKey;
	private final String bucket;
	private final RestClient restClient = RestClient.create();

	public StorageServiceImpl(
			@Value("${supabase.url}") String supabaseUrl,
			@Value("${supabase.service-role-key}") String serviceRoleKey,
			@Value("${supabase.storage.bucket}") String bucket) {
		this.supabaseUrl = supabaseUrl.replaceAll("/+$", "");
		this.serviceRoleKey = serviceRoleKey;
		this.bucket = bucket;
	}

	@Override
	public String upload(String path, byte[] content, String contentType) {
		if (!isConfigured()) {
			throw new ApiException(
					"Storage is not configured: set SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY",
					HttpStatus.SERVICE_UNAVAILABLE);
		}
		try {
			restClient.post()
					.uri(objectUrl(path))
					.header("Authorization", "Bearer " + serviceRoleKey)
					.contentType(MediaType.parseMediaType(contentType))
					.body(content)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException ex) {
			throw new ApiException("Failed to upload file to storage", HttpStatus.BAD_GATEWAY);
		}
		return publicUrlPrefix() + path;
	}

	@Override
	public void deleteByPublicUrl(String publicUrl) {
		if (publicUrl == null || !isConfigured() || !publicUrl.startsWith(publicUrlPrefix())) {
			return;
		}
		try {
			restClient.delete()
					.uri(objectUrl(publicUrl.substring(publicUrlPrefix().length())))
					.header("Authorization", "Bearer " + serviceRoleKey)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException ex) {
			throw new ApiException("Failed to delete file from storage", HttpStatus.BAD_GATEWAY);
		}
	}

	// Paths are server-generated (logo/<uuid>.<ext>), so plain concatenation is safe.
	private String objectUrl(String path) {
		return supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;
	}

	private String publicUrlPrefix() {
		return supabaseUrl + "/storage/v1/object/public/" + bucket + "/";
	}

	private boolean isConfigured() {
		return !supabaseUrl.isBlank() && !serviceRoleKey.isBlank();
	}
}
