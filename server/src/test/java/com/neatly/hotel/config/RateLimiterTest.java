package com.neatly.hotel.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

	private static final Instant START = Instant.parse("2026-09-16T00:00:00Z");

	/** A clock the test can move forward. */
	private static final class TestClock extends Clock {
		private Instant now = START;

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public java.time.ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}
	}

	private final TestClock clock = new TestClock();
	private final RateLimiter limiter = new RateLimiter(3, Duration.ofMinutes(1), clock);

	@Test
	void allowsUpToTheLimit() {
		for (int i = 0; i < 3; i++) {
			assertEquals(0, limiter.tryAcquire("1.1.1.1"));
		}
	}

	@Test
	void blocksTheNextRequestWithSecondsUntilReset() {
		for (int i = 0; i < 3; i++) {
			limiter.tryAcquire("1.1.1.1");
		}
		clock.advance(Duration.ofSeconds(20));

		assertEquals(40, limiter.tryAcquire("1.1.1.1"));
	}

	@Test
	void resetsAfterTheWindow() {
		for (int i = 0; i < 4; i++) {
			limiter.tryAcquire("1.1.1.1");
		}
		clock.advance(Duration.ofMinutes(1));

		assertEquals(0, limiter.tryAcquire("1.1.1.1"));
	}

	@Test
	void separateIpsAreIndependent() {
		for (int i = 0; i < 4; i++) {
			limiter.tryAcquire("1.1.1.1");
		}

		assertEquals(0, limiter.tryAcquire("2.2.2.2"));
	}

	@Test
	void removesStaleIpsAfterAWindow() {
		limiter.tryAcquire("1.1.1.1");
		limiter.tryAcquire("2.2.2.2");
		clock.advance(Duration.ofMinutes(1));

		limiter.tryAcquire("3.3.3.3");

		assertEquals(1, limiter.trackedKeys());
	}
}
