package com.neatly.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "chatbot_script")
public class ChatbotScript extends BaseEntity {

	@Column(nullable = false, columnDefinition = "text")
	private String greeting;

	@Column(name = "auto_reply", nullable = false, columnDefinition = "text")
	private String autoReply;

	/** JSON array of topics. Same text-JSON approach as booking standard requests. */
	@Column(nullable = false, columnDefinition = "text")
	private String topics = "[]";
}
