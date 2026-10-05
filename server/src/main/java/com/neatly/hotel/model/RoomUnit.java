package com.neatly.hotel.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "room_units")
public class RoomUnit extends BaseEntity {

	@Column(name = "room_number", nullable = false, unique = true, length = 10)
	private String roomNumber;

	@Column(nullable = false)
	private Short floor;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_type_id", nullable = false)
	private RoomType roomType;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_status_id", nullable = false)
	private RoomStatus roomStatus;

	private Instant deletedAt;
}
