package com.neatly.hotel;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import com.neatly.hotel.service.ProfileService;
import com.neatly.hotel.service.HotelInfoService;
import com.neatly.hotel.service.AnalyticsService;
import com.neatly.hotel.dto.AnalyticsResponse;
import com.neatly.hotel.dto.ProfileResponse;
import com.neatly.hotel.exception.ResourceNotFoundException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class HotelApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProfileService profileService;

	@MockitoBean
	private HotelInfoService hotelInfoService;

	@MockitoBean
	private AnalyticsService analyticsService;

	@Test
	void contextLoads() {
	}

	@Test
	void profileEndpointRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/profiles/me"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/profiles"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void hotelWritesRequireAuthentication() throws Exception {
		for (String path : new String[] { "/api/hotel", "/api/hotel/logo" }) {
			mockMvc.perform(put(path)).andExpect(status().isUnauthorized());
			mockMvc.perform(put(path).header("Authorization", "Bearer invalid-token"))
					.andExpect(status().isUnauthorized());
		}
		verifyNoInteractions(hotelInfoService);
	}

	@Test
	void tokenRoleCannotOverrideDatabaseUserRole() throws Exception {
		when(profileService.findByClerkUserId("user_customer")).thenReturn(profile("user_customer", "user"));
		for (String path : new String[] { "/api/hotel", "/api/hotel/logo" }) {
			mockMvc.perform(put(path).with(jwt().jwt(token -> token.subject("user_customer").claim("role", "agent"))))
					.andExpect(status().isForbidden());
		}
		verifyNoInteractions(hotelInfoService);
	}

	@Test
	void missingProfileCannotWriteHotel() throws Exception {
		when(profileService.findByClerkUserId("user_missing")).thenThrow(new ResourceNotFoundException("Profile not found"));
		for (String path : new String[] { "/api/hotel", "/api/hotel/logo" }) {
			mockMvc.perform(put(path).with(jwt().jwt(token -> token.subject("user_missing"))))
					.andExpect(status().isForbidden());
		}
		verifyNoInteractions(hotelInfoService);
	}

	@Test
	void agentCanWriteAndRoleRevocationTakesEffectImmediately() throws Exception {
		when(profileService.findByClerkUserId("user_agent")).thenReturn(profile("user_agent", "agent"), profile("user_agent", "user"));
		mockMvc.perform(put("/api/hotel").with(jwt().jwt(token -> token.subject("user_agent")))
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hotel\",\"description\":\"Updated description\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/hotel").with(jwt().jwt(token -> token.subject("user_agent"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void agentCanUploadHotelLogo() throws Exception {
		when(profileService.findByClerkUserId("user_agent")).thenReturn(profile("user_agent", "agent"));
		MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[] { 1, 2, 3 });
		mockMvc.perform(multipart(HttpMethod.PUT, "/api/hotel/logo").file(file)
				.with(jwt().jwt(token -> token.subject("user_agent"))))
				.andExpect(status().isOk());
		verify(hotelInfoService).replaceLogo(any());
	}

	@Test
	void publicHotelReadAndOpenApiRemainAccessible() throws Exception {
		mockMvc.perform(get("/api/hotel")).andExpect(status().isOk());
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/hotel'].put.security[0].clerkBearer").exists())
				.andExpect(jsonPath("$.paths['/api/hotel/logo'].put.responses['403']").exists())
				.andExpect(jsonPath("$.paths['/api/hotel'].get.security").doesNotExist());
	}

	@Test
	void promotionCodesRequireAgent() throws Exception {
		mockMvc.perform(get("/api/promotion-codes")).andExpect(status().isUnauthorized());
		when(profileService.findByClerkUserId("user_customer")).thenReturn(profile("user_customer", "user"));
		mockMvc.perform(get("/api/promotion-codes").with(jwt().jwt(token -> token.subject("user_customer"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void agentCanListSeededPromotionCode() throws Exception {
		when(profileService.findByClerkUserId("user_agent")).thenReturn(profile("user_agent", "agent"));
		mockMvc.perform(get("/api/promotion-codes").with(jwt().jwt(token -> token.subject("user_agent"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].code").value("NEATLYNEW400"))
				.andExpect(jsonPath("$.data[0].discountType").value("FIXED"))
				.andExpect(jsonPath("$.data[0].minPurchaseAmount").value(0));
	}

	@Test
	void bookingsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/bookings")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void analyticsRequiresAgentAndDocumentsSecurity() throws Exception {
		String path = "/api/admin/analytics?from=2026-09-01&to=2026-09-24";
		mockMvc.perform(get(path)).andExpect(status().isUnauthorized());

		when(profileService.findByClerkUserId("user_customer")).thenReturn(profile("user_customer", "user"));
		mockMvc.perform(get(path).with(jwt().jwt(token -> token.subject("user_customer"))))
				.andExpect(status().isForbidden());

		when(profileService.findByClerkUserId("user_agent")).thenReturn(profile("user_agent", "agent"));
		when(analyticsService.get(any(), any())).thenReturn(new AnalyticsResponse(
				new AnalyticsResponse.Period(null, null, null, null, "DAY"),
				new AnalyticsResponse.Summary(null, null, null),
				new AnalyticsResponse.RoomAvailability(0, 0, 0, 0),
				java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of()));
		mockMvc.perform(get(path).with(jwt().jwt(token -> token.subject("user_agent"))))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/admin/analytics?from=bad&to=2026-09-24")
				.with(jwt().jwt(token -> token.subject("user_agent"))))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/admin/analytics?from=2026-09-01")
				.with(jwt().jwt(token -> token.subject("user_agent"))))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/admin/analytics'].get.security[0].clerkBearer").exists())
				.andExpect(jsonPath("$.paths['/api/admin/analytics'].get.responses['403']").exists());
	}

	@Test
	void cashBookingCreatesConfirmedStay() throws Exception {
		String body = """
				{
				  "roomTypeId": "00000000-0000-0000-0001-000000000001",
				  "checkIn": "2026-10-19",
				  "checkOut": "2026-10-20",
				  "guests": 2,
				  "firstName": "Kate",
				  "lastName": "Cho",
				  "email": "kate@example.com",
				  "phoneNumber": "0812345678",
				  "country": "Thailand",
				  "dateOfBirth": "1990-01-15",
				  "specialRequestCodes": ["airport-transfer"],
				  "promotionCode": "NEATLYNEW400",
				  "paymentMethod": "CASH"
				}
				""";
		mockMvc.perform(post("/api/bookings").with(jwt().jwt(token -> token.subject("user_guest")))
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.status").value("CONFIRMED"))
				.andExpect(jsonPath("$.data.paymentMethod").value("CASH"))
				.andExpect(jsonPath("$.data.grandTotal").value(2300.0))
				.andExpect(jsonPath("$.data.clientSecret").doesNotExist());
	}

	private ProfileResponse profile(String subject, String role) {
		return new ProfileResponse(subject, null, null, null, null, null, null, role, null, null);
	}

}
