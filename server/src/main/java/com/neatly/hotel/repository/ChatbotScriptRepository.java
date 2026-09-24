package com.neatly.hotel.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neatly.hotel.model.ChatbotScript;

public interface ChatbotScriptRepository extends JpaRepository<ChatbotScript, UUID> {

	Optional<ChatbotScript> findFirstByOrderByCreatedAtAsc();
}
