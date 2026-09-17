package com.neatly.hotel.model;

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
@Table(name = "room_type_images")
public class RoomTypeImage extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_type_id", nullable = false)
	private RoomType roomType;

	@Column(nullable = false, columnDefinition = "text")
	private String url;

	@Column(nullable = false, columnDefinition = "text")
	private String storagePath;

	@Column(nullable = false)
	private Integer sortOrder = 0;

	@Column(nullable = false)
	private Boolean isMain = false;
}
