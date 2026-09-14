package com.neatly.hotel.service;

public interface StorageService {

	/** Uploads an object to the bucket and returns its public URL. */
	String upload(String path, byte[] content, String contentType);

	/** Uploads or replaces an object in a named bucket and returns its public URL. */
	String uploadOrReplace(String bucket, String path, byte[] content, String contentType);

	/** Deletes the object behind a public URL of this bucket. URLs from elsewhere are ignored. */
	void deleteByPublicUrl(String publicUrl);
}
