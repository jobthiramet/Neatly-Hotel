package com.neatly.hotel.service;

import com.neatly.hotel.model.Booking;

public interface MailService {

	/** Best-effort. A missing key or a provider failure must not undo the cancellation. */
	void sendCancellation(Booking booking, boolean refunded);
}
