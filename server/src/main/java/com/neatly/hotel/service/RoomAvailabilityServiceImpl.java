package com.neatly.hotel.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neatly.hotel.dto.AvailableRoomResponse;
import com.neatly.hotel.dto.RoomAvailabilityQuery;
import com.neatly.hotel.exception.ApiException;
import com.neatly.hotel.model.RoomType;
import com.neatly.hotel.repository.RoomTypeAvailability;
import com.neatly.hotel.repository.RoomTypeRepository;

@Service
@Transactional(readOnly = true)
public class RoomAvailabilityServiceImpl implements RoomAvailabilityService {

	/** Units in these statuses can't be sold. Housekeeping statuses (clean, dirty, …) don't matter for future stays. */
	static final List<String> BLOCKED_STATUSES = List.of("OUT_OF_ORDER", "OUT_OF_SERVICE");

	private final RoomTypeRepository roomTypeRepository;
	private final Clock clock;
	private final int maxNights;

	@Autowired
	public RoomAvailabilityServiceImpl(
			RoomTypeRepository roomTypeRepository,
			@Value("${app.hotel.time-zone:Asia/Bangkok}") String timeZone,
			@Value("${app.search.max-nights:30}") int maxNights) {
		this(roomTypeRepository, Clock.system(ZoneId.of(timeZone)), maxNights);
	}

	RoomAvailabilityServiceImpl(RoomTypeRepository roomTypeRepository, Clock clock, int maxNights) {
		this.roomTypeRepository = roomTypeRepository;
		this.clock = clock;
		this.maxNights = maxNights;
	}

	@Override
	public List<AvailableRoomResponse> search(RoomAvailabilityQuery query) {
		if (query.checkIn().isBefore(LocalDate.now(clock))) {
			throw new ApiException("checkIn: must not be in the past (hotel time, " + clock.getZone() + ")",
					HttpStatus.BAD_REQUEST);
		}
		if (ChronoUnit.DAYS.between(query.checkIn(), query.checkOut()) > maxNights) {
			throw new ApiException("checkOut: stay must be at most " + maxNights + " nights", HttpStatus.BAD_REQUEST);
		}

		Map<UUID, Long> available = roomTypeRepository.availability(
				query.checkIn(),
				query.checkOut(),
				query.rooms(),
				query.guests(),
				clock.instant(),
				BookingServiceImpl.OCCUPYING,
				BLOCKED_STATUSES).stream()
				.filter(row -> row.availableUnits() >= query.rooms())
				.collect(Collectors.toMap(RoomTypeAvailability::roomTypeId, RoomTypeAvailability::availableUnits));
		if (available.isEmpty()) {
			return List.of();
		}

		return roomTypeRepository.findByIdIn(available.keySet()).stream()
				.sorted(Comparator.comparing(RoomType::getPricePerNight).thenComparing(RoomType::getName))
				.map(room -> AvailableRoomResponse.from(room, available.get(room.getId())))
				.toList();
	}
}
