package com.neatly.hotel.service;

import java.time.LocalDate;

import com.neatly.hotel.dto.AnalyticsResponse;

public interface AnalyticsService {

	AnalyticsResponse get(LocalDate from, LocalDate to);
}
