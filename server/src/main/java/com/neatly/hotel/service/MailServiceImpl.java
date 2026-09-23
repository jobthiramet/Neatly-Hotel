package com.neatly.hotel.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.neatly.hotel.model.Booking;

/** Sends transactional mail through the Brevo HTTP API. The API key stays on the server. */
@Service
public class MailServiceImpl implements MailService {

	private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);
	private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

	private final String apiKey;
	private final Sender sender;
	private final RestClient restClient = RestClient.create();

	public MailServiceImpl(
			@Value("${brevo.api-key:}") String apiKey,
			@Value("${mail.from:}") String from) {
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.sender = parseFrom(from);
	}

	@Override
	public void sendCancellation(Booking booking, boolean refunded) {
		if (apiKey.isBlank()) {
			log.info("Cancellation email skipped: BREVO_API_KEY is not set (booking {})", booking.getBookingNumber());
			return;
		}
		if (sender.email().isBlank()) {
			log.info("Cancellation email skipped: MAIL_FROM is not set (booking {})", booking.getBookingNumber());
			return;
		}
		String to = booking.getGuestEmail();
		if (to == null || to.isBlank()) {
			log.warn("Cancellation email skipped: booking {} has no guest email", booking.getBookingNumber());
			return;
		}
		try {
			restClient.post()
					.uri(BREVO_URL)
					.header("api-key", apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(new BrevoEmail(sender, List.of(new Recipient(to)), subject(booking), body(booking, refunded)))
					.retrieve()
					.toBodilessEntity();
		} catch (RestClientException ex) {
			log.warn("Cancellation email failed for booking {}: {}", booking.getBookingNumber(), ex.getMessage());
		}
	}

	private static String subject(Booking booking) {
		return "Neatly Hotel: booking " + booking.getBookingNumber() + " cancelled";
	}

	private static String body(Booking booking, boolean refunded) {
		String guests = booking.getGuests() == 1 ? "1 guest" : booking.getGuests() + " guests";
		String outcome = refunded
				? "A refund of " + booking.getCurrency() + " " + booking.getGrandTotal() + " will be returned to your original payment method."
				: "This cancellation is not eligible for a refund.";
		return """
				Hello %s %s,

				Your booking %s for %s has been cancelled.

				Check-in: %s
				Check-out: %s
				Guests: %s

				%s
				""".formatted(
				booking.getGuestFirstName(),
				booking.getGuestLastName(),
				booking.getBookingNumber(),
				booking.getRoomNameSnapshot(),
				booking.getCheckIn(),
				booking.getCheckOut(),
				guests,
				outcome);
	}

	/** Accepts `Name <email@host>` or a bare email address. */
	private static Sender parseFrom(String raw) {
		String value = raw == null ? "" : raw.trim();
		int open = value.lastIndexOf('<');
		int close = value.lastIndexOf('>');
		if (open >= 0 && close > open) {
			String name = value.substring(0, open).trim();
			String email = value.substring(open + 1, close).trim();
			return new Sender(name.isEmpty() ? "Neatly Hotel" : name, email);
		}
		return new Sender("Neatly Hotel", value);
	}

	private record BrevoEmail(Sender sender, List<Recipient> to, String subject, String textContent) {
	}

	private record Sender(String name, String email) {
	}

	private record Recipient(String email) {
	}
}
