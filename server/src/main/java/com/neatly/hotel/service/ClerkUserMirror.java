package com.neatly.hotel.service;

/** Mirrors the Clerk-owned profile fields to Clerk after a successful database save. */
public interface ClerkUserMirror {

	/** True when Clerk was updated, false when the call failed, null when mirroring is not configured. */
	Boolean mirrorName(String clerkUserId, String firstName, String lastName);
}
