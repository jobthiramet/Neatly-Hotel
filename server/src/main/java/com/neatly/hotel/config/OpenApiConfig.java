package com.neatly.hotel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI neatlyHotelOpenApi() {
		return new OpenAPI()
				.components(new Components().addSecuritySchemes(
						"clerkBearer",
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
				.info(new Info()
						.title("Neatly Hotel API")
						.description("Spring Boot API for Neatly Hotel (Supabase PostgreSQL)")
						.version("v0.0.1")
						.contact(new Contact().name("Neatly Hotel Team")));
	}
}
