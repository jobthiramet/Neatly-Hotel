package com.neatly.hotel.service;

import org.springframework.web.multipart.MultipartFile;

import com.neatly.hotel.dto.HotelInfoResponse;
import com.neatly.hotel.dto.UpdateHotelInfoRequest;

public interface HotelInfoService {

	HotelInfoResponse get();

	HotelInfoResponse update(UpdateHotelInfoRequest request);

	HotelInfoResponse replaceLogo(MultipartFile file);
}
