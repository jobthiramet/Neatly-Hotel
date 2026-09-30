package com.neatly.hotel.dto;

/**
 * The saved profile plus whether the Clerk mirror succeeded.
 * {@code clerkSynced} is null when Clerk mirroring is not configured.
 */
public record ProfileUpdateResponse(ProfileResponse profile, Boolean clerkSynced) {
}
