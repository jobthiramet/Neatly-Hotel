package com.neatly.hotel.config;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

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
 * Per-IP rate limit for POST /api/chatbot/ask only.
 * Separate from {@link RateLimitFilter}, which covers public room GETs.
 * Uses the socket address only: X-Forwarded-For is not trusted.
 */
@Component
public class ChatbotAskRateLimitFilter extends OncePerRequestFilter {

	static final String PATH = "/api/chatbot/ask";

	private final RateLimiter limiter;
	private final HandlerExceptionResolver exceptionResolver;

	public ChatbotAskRateLimitFilter(
			@Value("${app.chatbot.ask-rate-limit.max-requests:10}") int maxRequests,
			@Value("${app.chatbot.ask-rate-limit.window:1m}") Duration window,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
		this.limiter = new RateLimiter(maxRequests, window, Clock.systemUTC());
		this.exceptionResolver = exceptionResolver;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !"POST".equals(request.getMethod()) || !PATH.equals(request.getRequestURI());
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
		exceptionResolver.resolveException(request, response, null,
				new ApiException("Too many requests, please try again later", HttpStatus.TOO_MANY_REQUESTS));
	}
}
