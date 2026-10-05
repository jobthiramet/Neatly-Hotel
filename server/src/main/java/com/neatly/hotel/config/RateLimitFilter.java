package com.neatly.hotel.config;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.neatly.hotel.exception.ApiException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Per-IP rate limit on the GET paths in {@code app.rate-limit.paths} (exact match).
 * Uses the socket address only: no trusted-proxy config exists, so X-Forwarded-For is not trusted.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

	private final RateLimiter limiter;
	private final List<String> paths;
	private final HandlerExceptionResolver exceptionResolver;

	public RateLimitFilter(
			@Value("${app.rate-limit.max-requests:60}") int maxRequests,
			@Value("${app.rate-limit.window:1m}") Duration window,
			@Value("${app.rate-limit.paths:/api/rooms}") List<String> paths,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
		this.limiter = new RateLimiter(maxRequests, window, Clock.systemUTC());
		this.paths = paths;
		this.exceptionResolver = exceptionResolver;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !"GET".equals(request.getMethod()) || !paths.contains(request.getRequestURI());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		long retryAfterSeconds = limiter.tryAcquire(request.getRemoteAddr());
		if (retryAfterSeconds == 0) {
			chain.doFilter(request, response);
			return;
		}
		response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
		// Hand off to GlobalExceptionHandler so the body matches every other error response.
		exceptionResolver.resolveException(request, response, null,
				new ApiException("Too many requests, please try again later", HttpStatus.TOO_MANY_REQUESTS));
	}
}
