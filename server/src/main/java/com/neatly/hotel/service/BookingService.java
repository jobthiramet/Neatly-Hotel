package com.neatly.hotel.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.neatly.hotel.dto.BookingResponse;
import com.neatly.hotel.dto.ChangeBookingDatesRequest;
import com.neatly.hotel.dto.CreateBookingRequest;
import com.neatly.hotel.dto.PageResponse;

public interface BookingService {

	BookingResponse create(String clerkUserId, CreateBookingRequest request);

	BookingResponse retryPayment(String clerkUserId, UUID bookingId);

	PageResponse<BookingResponse> listMine(String clerkUserId, Pageable pageable);

	BookingResponse findMine(String clerkUserId, UUID bookingId);

	BookingResponse cancel(String clerkUserId, UUID bookingId);

	BookingResponse changeDates(String clerkUserId, UUID bookingId, ChangeBookingDatesRequest request);

	void handleStripeEvent(String payload, String signature);
}
