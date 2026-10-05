package com.neatly.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AnalyticsResponse(
		Period period,
		Summary summary,
		RoomAvailability roomAvailability,
		List<TrendPoint> bookingTrend,
		List<TrendPoint> revenueTrend,
		List<Breakdown> guestMix,
		List<Breakdown> paymentMethods) {

	public record Period(
			LocalDate from,
			LocalDate to,
			LocalDate comparisonFrom,
			LocalDate comparisonTo,
			String granularity) {}

	public record Metric(BigDecimal value, BigDecimal previousValue, BigDecimal changePercent) {}

	public record Summary(Metric totalBookings, Metric totalSales, Metric bookingUsers) {}

	public record RoomAvailability(long occupied, long booked, long available, long totalBookable) {}

	public record TrendPoint(String label, BigDecimal value) {}

	public record Breakdown(String key, long count, BigDecimal percentage) {}
}
