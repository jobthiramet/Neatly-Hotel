package com.neatly.hotel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import com.neatly.hotel.exception.ResourceNotFoundException;
import com.neatly.hotel.service.ProfileService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			ProfileService profileService,
			@Value("${clerk.issuer:}") String issuer,
			@Value("${clerk.authorized-party:}") String authorizedParty) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.GET, "/api/admin/analytics")
						.access((authentication, context) -> new AuthorizationDecision(isAgent(authentication.get(), profileService)))
						.requestMatchers(HttpMethod.PUT, "/api/hotel", "/api/hotel/**")
						.access((authentication, context) -> new AuthorizationDecision(isAgent(authentication.get(), profileService)))
						.requestMatchers("/api/promotion-codes", "/api/promotion-codes/**")
						.access((authentication, context) -> new AuthorizationDecision(isAgent(authentication.get(), profileService)))
						.requestMatchers("/api/profiles/**", "/api/bookings/**").authenticated()
						.requestMatchers(
								"/api/health",
								"/api/stripe/webhooks",
								"/v3/api-docs/**",
								"/swagger-ui/**",
								"/swagger-ui.html")
						.permitAll()
						.anyRequest().permitAll());

		JwtDecoder decoder = issuer.isBlank()
				? token -> { throw new BadJwtException("Clerk issuer is not configured"); }
				: clerkJwtDecoder(issuer, authorizedParty);
		http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(decoder)));

		return http.build();
	}

	private boolean isAgent(Authentication authentication, ProfileService profileService) {
		if (!(authentication instanceof JwtAuthenticationToken jwt) || !authentication.isAuthenticated()) {
			return false;
		}
		try {
			return "agent".equals(profileService.findByClerkUserId(jwt.getToken().getSubject()).role());
		} catch (ResourceNotFoundException exception) {
			return false;
		}
	}

	private JwtDecoder clerkJwtDecoder(String issuer, String authorizedParty) {
		String normalizedIssuer = issuer.replaceAll("/+$", "");
		NimbusJwtDecoder decoder = NimbusJwtDecoder
				.withJwkSetUri(normalizedIssuer + "/.well-known/jwks.json")
				.build();
		OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(normalizedIssuer);
		if (!authorizedParty.isBlank()) {
			OAuth2Error error = new OAuth2Error("invalid_token", "Invalid authorized party", null);
			OAuth2TokenValidator<Jwt> authorizedPartyValidator = token -> authorizedParty.equals(token.getClaimAsString("azp"))
					? OAuth2TokenValidatorResult.success()
					: OAuth2TokenValidatorResult.failure(error);
			validator = new DelegatingOAuth2TokenValidator<>(validator, authorizedPartyValidator);
		}
		decoder.setJwtValidator(validator);
		return decoder;
	}
}
