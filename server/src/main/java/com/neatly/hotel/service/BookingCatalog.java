package com.neatly.hotel.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Figma checkout extras. Prices are THB per night and snapshotted onto booking_items. */
public final class BookingCatalog {

	public record Addon(String code, String label, BigDecimal price) {
	}

	public record Preference(String code, String label) {
	}

	public static final List<Addon> ADDONS = List.of(
			new Addon("baby-cot", "Baby cot", new BigDecimal("400.00")),
			new Addon("airport-transfer", "Airport transfer", new BigDecimal("200.00")),
			new Addon("extra-bed", "Extra bed", new BigDecimal("500.00")),
			new Addon("extra-pillows", "Extra pillows", new BigDecimal("100.00")),
			new Addon("phone-chargers", "Phone chargers and adapters", new BigDecimal("100.00")),
			new Addon("breakfast", "Breakfast", new BigDecimal("150.00")));

	public static final List<Preference> PREFERENCES = List.of(
			new Preference("early-check-in", "Early check-in"),
			new Preference("late-check-out", "Late check-out"),
			new Preference("non-smoking", "Non-smoking room"),
			new Preference("high-floor", "A room on the high floor"),
			new Preference("quiet-room", "A quiet room"));

	public static final String CHECK_IN_TIME_TEXT = "After 2:00 PM";
	public static final String CHECK_OUT_TIME_TEXT = "Before 12:00 PM";
	public static final int HOLD_SECONDS = 5 * 60;

	private static final Map<String, Addon> ADDONS_BY_CODE = indexAddons();
	private static final Map<String, Preference> PREFERENCES_BY_CODE = indexPreferences();

	private BookingCatalog() {
	}

	public static Optional<Addon> addon(String code) {
		return Optional.ofNullable(ADDONS_BY_CODE.get(code));
	}

	public static Optional<Preference> preference(String code) {
		return Optional.ofNullable(PREFERENCES_BY_CODE.get(code));
	}

	private static Map<String, Addon> indexAddons() {
		Map<String, Addon> map = new LinkedHashMap<>();
		ADDONS.forEach(addon -> map.put(addon.code(), addon));
		return Map.copyOf(map);
	}

	private static Map<String, Preference> indexPreferences() {
		Map<String, Preference> map = new LinkedHashMap<>();
		PREFERENCES.forEach(preference -> map.put(preference.code(), preference));
		return Map.copyOf(map);
	}
}
