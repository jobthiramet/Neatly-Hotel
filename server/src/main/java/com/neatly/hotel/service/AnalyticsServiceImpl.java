package com.neatly.hotel.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neatly.hotel.dto.AnalyticsResponse;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.model.Booking;
import com.neatly.hotel.model.BookingPaymentMethod;
import com.neatly.hotel.model.BookingStatus;
import com.neatly.hotel.repository.BookingRepository;
import com.neatly.hotel.repository.RoomUnitRepository;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

	static final List<BookingStatus> INCLUDED_STATUSES = List.of(
			BookingStatus.CONFIRMED,
			BookingStatus.CHECKED_IN,
			BookingStatus.CHECKED_OUT,
			BookingStatus.COMPLETED);
	private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("d MMM");
	private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy");

	private final BookingRepository bookingRepository;
	private final RoomUnitRepository roomUnitRepository;
	private final Clock clock;

	@Autowired
	public AnalyticsServiceImpl(
			BookingRepository bookingRepository,
			RoomUnitRepository roomUnitRepository,
			@Value("${app.hotel.time-zone:Asia/Bangkok}") String timeZone) {
		this(bookingRepository, roomUnitRepository, Clock.system(ZoneId.of(timeZone)));
	}

	AnalyticsServiceImpl(BookingRepository bookingRepository, RoomUnitRepository roomUnitRepository, Clock clock) {
		this.bookingRepository = bookingRepository;
		this.roomUnitRepository = roomUnitRepository;
		this.clock = clock;
	}

	@Override
	public AnalyticsResponse get(LocalDate from, LocalDate to) {
		validate(from, to);
		long days = ChronoUnit.DAYS.between(from, to) + 1;
		LocalDate comparisonTo = from.minusDays(1);
		LocalDate comparisonFrom = comparisonTo.minusDays(days - 1);

		List<Booking> current = bookings(from, to);
		List<Booking> previous = bookings(comparisonFrom, comparisonTo);
		Set<String> priorUsers = new HashSet<>(bookingRepository.findDistinctUserIdsBefore(
				INCLUDED_STATUSES,
				startOfDay(from)));

		AnalyticsResponse.Summary summary = new AnalyticsResponse.Summary(
				metric(BigDecimal.valueOf(current.size()), BigDecimal.valueOf(previous.size())),
				metric(totalSales(current), totalSales(previous)),
				metric(BigDecimal.valueOf(distinctUsers(current)), BigDecimal.valueOf(distinctUsers(previous))));

		LocalDate today = LocalDate.now(clock);
		Instant now = clock.instant();
		long totalBookable = roomUnitRepository.countBookable(RoomAvailabilityServiceImpl.BLOCKED_STATUSES);
		long occupied = bookingRepository.countOccupiedRooms(today, BookingStatus.CHECKED_IN);
		long booked = bookingRepository.countBookedRooms(
				today,
				now,
				List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING_PAYMENT));
		long available = Math.max(totalBookable - occupied - booked, 0);

		boolean daily = days <= 31;
		return new AnalyticsResponse(
				new AnalyticsResponse.Period(from, to, comparisonFrom, comparisonTo, daily ? "DAY" : "MONTH"),
				summary,
				new AnalyticsResponse.RoomAvailability(occupied, booked, available, totalBookable),
				trend(current, from, to, daily, false),
				trend(current, from, to, daily, true),
				guestMix(current, priorUsers),
				paymentMethods(current));
	}

	private List<Booking> bookings(LocalDate from, LocalDate to) {
		return bookingRepository.findAnalyticsBookings(
				INCLUDED_STATUSES,
				startOfDay(from),
				startOfDay(to.plusDays(1)));
	}

	private Instant startOfDay(LocalDate date) {
		return date.atStartOfDay(clock.getZone()).toInstant();
	}

	private void validate(LocalDate from, LocalDate to) {
		if (from == null || to == null) {
			throw new ApiException("from and to are required", HttpStatus.BAD_REQUEST);
		}
		if (from.isAfter(to)) {
			throw new ApiException("from must be on or before to", HttpStatus.BAD_REQUEST);
		}
		if (to.isAfter(LocalDate.now(clock))) {
			throw new ApiException("to must not be in the future", HttpStatus.BAD_REQUEST);
		}
		if (ChronoUnit.DAYS.between(from, to) + 1 > 366) {
			throw new ApiException("date range must not exceed 366 days", HttpStatus.BAD_REQUEST);
		}
	}

	private AnalyticsResponse.Metric metric(BigDecimal value, BigDecimal previous) {
		BigDecimal change = previous.signum() == 0
				? null
				: value.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 1, RoundingMode.HALF_UP);
		return new AnalyticsResponse.Metric(value, previous, change);
	}

	private BigDecimal totalSales(List<Booking> bookings) {
		return bookings.stream().map(Booking::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private long distinctUsers(List<Booking> bookings) {
		return bookings.stream().map(Booking::getUserId).distinct().count();
	}

	private List<AnalyticsResponse.TrendPoint> trend(
			List<Booking> bookings,
			LocalDate from,
			LocalDate to,
			boolean daily,
			boolean revenue) {
		Map<String, BigDecimal> values = new LinkedHashMap<>();
		if (daily) {
			for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
				values.put(date.toString(), BigDecimal.ZERO);
			}
		} else {
			for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(to)); month = month.plusMonths(1)) {
				values.put(month.toString(), BigDecimal.ZERO);
			}
		}
		for (Booking booking : bookings) {
			LocalDate date = booking.getCreatedAt().atZone(clock.getZone()).toLocalDate();
			String key = daily ? date.toString() : YearMonth.from(date).toString();
			BigDecimal amount = revenue ? booking.getGrandTotal() : BigDecimal.ONE;
			values.computeIfPresent(key, (ignored, current) -> current.add(amount));
		}
		return values.entrySet().stream()
				.map(entry -> new AnalyticsResponse.TrendPoint(
						daily ? LocalDate.parse(entry.getKey()).format(DAY_LABEL) : YearMonth.parse(entry.getKey()).format(MONTH_LABEL),
						entry.getValue()))
				.toList();
	}

	private List<AnalyticsResponse.Breakdown> guestMix(List<Booking> bookings, Set<String> priorUsers) {
		Set<String> users = new HashSet<>();
		bookings.forEach(booking -> users.add(booking.getUserId()));
		long returning = users.stream().filter(priorUsers::contains).count();
		return breakdown(Map.of("NEW", users.size() - returning, "RETURNING", returning), List.of("NEW", "RETURNING"));
	}

	private List<AnalyticsResponse.Breakdown> paymentMethods(List<Booking> bookings) {
		Map<BookingPaymentMethod, Long> counts = new EnumMap<>(BookingPaymentMethod.class);
		bookings.forEach(booking -> counts.merge(booking.getPaymentMethod(), 1L, Long::sum));
		return breakdown(
				Map.of(
						"STRIPE", counts.getOrDefault(BookingPaymentMethod.STRIPE, 0L),
						"CASH", counts.getOrDefault(BookingPaymentMethod.CASH, 0L)),
				List.of("STRIPE", "CASH"));
	}

	private List<AnalyticsResponse.Breakdown> breakdown(Map<String, Long> counts, List<String> order) {
		long total = counts.values().stream().mapToLong(Long::longValue).sum();
		List<AnalyticsResponse.Breakdown> result = new ArrayList<>();
		for (String key : order) {
			long count = counts.getOrDefault(key, 0L);
			BigDecimal percentage = total == 0
					? BigDecimal.ZERO
					: BigDecimal.valueOf(count * 100L).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
			result.add(new AnalyticsResponse.Breakdown(key, count, percentage));
		}
		return result;
	}
}
