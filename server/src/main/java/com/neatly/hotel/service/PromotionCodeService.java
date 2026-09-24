package com.neatly.hotel.service;

import java.util.List;
import java.util.UUID;

import com.neatly.hotel.dto.PromotionCodeRequest;
import com.neatly.hotel.dto.PromotionCodeResponse;

public interface PromotionCodeService {

	List<PromotionCodeResponse> findAll();

	PromotionCodeResponse findById(UUID id);

	PromotionCodeResponse create(PromotionCodeRequest request);

	PromotionCodeResponse update(UUID id, PromotionCodeRequest request);

	void delete(UUID id);
}
