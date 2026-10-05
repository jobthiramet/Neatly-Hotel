package com.neatly.hotel.config;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fixed-window request counter per client key (IP).
 * ponytail: in-memory and per instance; move to a shared store (e.g. Redis) or the gateway if the server is scaled horizontally.
 */
public class RateLimiter {

	private record Window(long startMillis, int count) {
	}

	private final int maxRequests;
	private final long windowMillis;
	private final Clock clock;
	private final Map<String, Window> windows = new ConcurrentHashMap<>();
	private final AtomicLong lastCleanupMillis;

	public RateLimiter(int maxRequests, Duration window, Clock clock) {
		this.maxRequests = maxRequests;
		this.windowMillis = window.toMillis();
		this.clock = clock;
		this.lastCleanupMillis = new AtomicLong(clock.millis());
	}

	/** Counts a request. Returns 0 when allowed, otherwise the seconds until the key's window resets. */
	public long tryAcquire(String key) {
		long now = clock.millis();
		cleanupExpired(now);
		Window window = windows.compute(key, (k, current) -> current == null || now - current.startMillis() >= windowMillis
				? new Window(now, 1)
				: new Window(current.startMillis(), current.count() + 1));
		if (window.count() <= maxRequests) {
			return 0;
		}
		long remainingMillis = window.startMillis() + windowMillis - now;
		return Math.max(1, (remainingMillis + 999) / 1000);
	}

	int trackedKeys() {
		return windows.size();
	}

	// Runs at most once per window, on a request thread, so stale IPs don't grow the map.
	private void cleanupExpired(long now) {
		long last = lastCleanupMillis.get();
		if (now - last >= windowMillis && lastCleanupMillis.compareAndSet(last, now)) {
			windows.values().removeIf(window -> now - window.startMillis() >= windowMillis);
		}
	}
}
