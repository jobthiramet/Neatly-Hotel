package com.neatly.hotel.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.neatly.hotel.dto.BookingResponse;
import com.neatly.hotel.dto.ChangeBookingDatesRequest;
import com.neatly.hotel.dto.CreateBookingRequest;
import com.neatly.hotel.dto.PageResponse;
import com.neatly.hotel.dto.UpdatePromotionCodeRequest;

public interface BookingService {

	BookingResponse create(String clerkUserId, CreateBookingRequest request);

	BookingResponse retryPayment(String clerkUserId, UUID bookingId);

	PageResponse<BookingResponse> listMine(String clerkUserId, Pageable pageable);

	BookingResponse findMine(String clerkUserId, UUID bookingId);

	BookingResponse cancel(String clerkUserId, UUID bookingId);

	BookingResponse changeDates(String clerkUserId, UUID bookingId, ChangeBookingDatesRequest request);

	/** Reprices an open card draft and updates its Checkout Session. The client secret does not change. */
	BookingResponse updatePromotion(String clerkUserId, UUID bookingId, UpdatePromotionCodeRequest request);

	void handleStripeEvent(String payload, String signature);
}
