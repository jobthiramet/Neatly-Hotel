package com.neatly.hotel.service;

public interface StorageService {

	/** Uploads an object to the bucket and returns its public URL. */
	String upload(String path, byte[] content, String contentType);

	/** Deletes the object behind a public URL of this bucket. URLs from elsewhere are ignored. */
	void deleteByPublicUrl(String publicUrl);
}
