package com.neatly.hotel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI neatlyHotelOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Neatly Hotel API")
						.description("Spring Boot API for Neatly Hotel (Supabase PostgreSQL)")
						.version("v0.0.1")
						.contact(new Contact().name("Neatly Hotel Team")));
	}
}
