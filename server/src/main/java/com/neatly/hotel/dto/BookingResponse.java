package com.neatly.hotel.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neatly.hotel.model.Booking;
import com.neatly.hotel.model.BookingItemKind;
import com.neatly.hotel.model.BookingPaymentMethod;
import com.neatly.hotel.model.BookingStatus;
import com.neatly.hotel.model.PaymentKind;
import com.neatly.hotel.model.PaymentStatus;
import com.neatly.hotel.service.BookingCatalog;

public record BookingResponse(
		UUID id,
		String bookingNumber,
		UUID roomTypeId,
		String roomName,
		String roomImageUrl,
		LocalDate checkIn,
		LocalDate checkOut,
		String checkInTimeText,
		String checkOutTimeText,
		int guests,
		int nights,
		int roomsCount,
		BookingStatus status,
		BookingPaymentMethod paymentMethod,
		String guestFirstName,
		String guestLastName,
		String guestEmail,
		String guestPhone,
		String guestCountry,
		LocalDate guestDateOfBirth,
		List<NamedRequest> standardRequests,
		String additionalRequest,
		String promotionCode,
		String currency,
		List<LineItem> items,
		BigDecimal roomSubtotal,
		BigDecimal extrasTotal,
		BigDecimal discountTotal,
		BigDecimal grandTotal,
		String paymentMethodText,
		String clientSecret,
		Instant holdExpiresAt,
		Instant cancelledAt,
		Instant createdAt,
		Instant updatedAt) {

	public record NamedRequest(String code, String label) {
	}

	public record LineItem(BookingItemKind kind, String code, String label, int quantity, BigDecimal unitPrice, BigDecimal amount) {
	}

	public static BookingResponse from(Booking booking, String clientSecret, ObjectMapper mapper) {
		List<NamedRequest> preferences = readPreferences(booking.getStandardRequests(), mapper);
		PaymentStatus chargeStatus = booking.getPayments().stream()
				.filter(payment -> payment.getKind() == PaymentKind.CHARGE)
				.reduce((first, second) -> second)
				.map(com.neatly.hotel.model.Payment::getStatus)
				.orElse(null);
		return new BookingResponse(
				booking.getId(),
				booking.getBookingNumber(),
				booking.getRoomType().getId(),
				booking.getRoomNameSnapshot(),
				booking.getRoomImageUrl(),
				booking.getCheckIn(),
				booking.getCheckOut(),
				BookingCatalog.CHECK_IN_TIME_TEXT,
				BookingCatalog.CHECK_OUT_TIME_TEXT,
				booking.getGuests(),
				(int) booking.getCheckOut().toEpochDay() - (int) booking.getCheckIn().toEpochDay(),
				booking.getRoomsCount(),
				booking.getStatus(),
				booking.getPaymentMethod(),
				booking.getGuestFirstName(),
				booking.getGuestLastName(),
				booking.getGuestEmail(),
				booking.getGuestPhone(),
				booking.getGuestCountry(),
				booking.getGuestDateOfBirth(),
				preferences,
				booking.getAdditionalRequest(),
				booking.getPromotionCode() == null ? null : booking.getPromotionCode().getCode(),
				booking.getCurrency(),
				booking.getItems().stream()
						.map(item -> new LineItem(
								item.getKind(),
								item.getCode(),
								item.getLabel(),
								item.getQuantity(),
								item.getUnitPrice(),
								item.getAmount()))
						.toList(),
				booking.getRoomSubtotal(),
				booking.getExtrasTotal(),
				booking.getDiscountTotal(),
				booking.getGrandTotal(),
				paymentMethodText(booking, chargeStatus),
				clientSecret,
				booking.getHoldExpiresAt(),
				booking.getCancelledAt(),
				booking.getCreatedAt(),
				booking.getUpdatedAt());
	}

	private static String paymentMethodText(Booking booking, PaymentStatus chargeStatus) {
		if (booking.getPaymentMethod() == BookingPaymentMethod.CASH) {
			return chargeStatus == PaymentStatus.SUCCEEDED
					? "Payment success via Cash"
					: "Pay at the hotel with cash or cheque";
		}
		return booking.getPayments().stream()
				.filter(payment -> payment.getKind() == PaymentKind.CHARGE && payment.getCardLast4() != null)
				.reduce((first, second) -> second)
				.map(payment -> "Payment success via Credit Card - *" + payment.getCardLast4())
				.orElse(chargeStatus == PaymentStatus.SUCCEEDED
						? "Payment success via Credit Card"
						: "Credit Card");
	}

	private static List<NamedRequest> readPreferences(String json, ObjectMapper mapper) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			return mapper.readValue(json, new TypeReference<List<NamedRequest>>() {
			});
		} catch (Exception ex) {
			return List.of();
		}
	}
}
