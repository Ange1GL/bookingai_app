package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.domain.model.CustomerBlacklist;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.CustomerBlacklistEntity;

@Component
public class CustomerBlacklistMapper {

	public CustomerBlacklistEntity toEntity(CustomerBlacklist entry) {
		CustomerBlacklistEntity entity = new CustomerBlacklistEntity();
		entity.setCustomerId(entry.getCustomerId());
		entity.setUserId(entry.getUserId());
		entity.setReason(entry.getReason());
		entity.setCreatedAt(entry.getCreatedAt());
		return entity;
	}

	public CustomerBlacklist toDomain(CustomerBlacklistEntity entity) {
		return CustomerBlacklist.reconstitute(
				entity.getCustomerId(), entity.getUserId(), entity.getReason(), entity.getCreatedAt());
	}
}
