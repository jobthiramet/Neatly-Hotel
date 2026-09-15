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
		return upload(bucket, path, content, contentType, false);
	}

	@Override
	public String uploadOrReplace(String bucket, String path, byte[] content, String contentType) {
		return upload(bucket, path, content, contentType, true);
	}

	private String upload(String targetBucket, String path, byte[] content, String contentType, boolean upsert) {
		if (!isConfigured()) {
			throw new ApiException(
					"Storage is not configured: set SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY",
					HttpStatus.SERVICE_UNAVAILABLE);
		}
		try {
			RestClient.RequestBodySpec request = restClient.post()
					.uri(objectUrl(targetBucket, path))
					.header("Authorization", "Bearer " + serviceRoleKey)
					.contentType(MediaType.parseMediaType(contentType));
			if (upsert) {
				request.header("x-upsert", "true");
			}
			request
					.body(content)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException ex) {
			throw new ApiException("Failed to upload file to storage", HttpStatus.BAD_GATEWAY);
		}
		return publicUrlPrefix(targetBucket) + path;
	}

	@Override
	public void deleteByPublicUrl(String publicUrl) {
		if (publicUrl == null || !isConfigured() || !publicUrl.startsWith(publicUrlPrefix())) {
			return;
		}
		try {
			restClient.delete()
					.uri(objectUrl(bucket, publicUrl.substring(publicUrlPrefix().length())))
					.header("Authorization", "Bearer " + serviceRoleKey)
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException ex) {
			throw new ApiException("Failed to delete file from storage", HttpStatus.BAD_GATEWAY);
		}
	}

	// Paths and bucket names are server-generated, so plain concatenation is safe.
	private String objectUrl(String targetBucket, String path) {
		return supabaseUrl + "/storage/v1/object/" + targetBucket + "/" + path;
	}

	private String publicUrlPrefix() {
		return publicUrlPrefix(bucket);
	}

	private String publicUrlPrefix(String targetBucket) {
		return supabaseUrl + "/storage/v1/object/public/" + targetBucket + "/";
	}

	private boolean isConfigured() {
		return !supabaseUrl.isBlank() && !serviceRoleKey.isBlank();
	}
}
