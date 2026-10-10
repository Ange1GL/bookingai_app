package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.domain.model.NoShow;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.CustomerNoShowEntity;

@Component
public class NoShowMapper {

	public CustomerNoShowEntity toEntity(NoShow noShow) {
		CustomerNoShowEntity entity = new CustomerNoShowEntity();
		entity.setCustomerId(noShow.getCustomerId());
		entity.setAppointmentId(noShow.getAppointmentId());
		entity.setUserId(noShow.getUserId());
		entity.setReason(noShow.getReason());
		entity.setCreatedAt(noShow.getCreatedAt());
		return entity;
	}

	public NoShow toDomain(CustomerNoShowEntity entity) {
		return NoShow.reconstitute(
				entity.getId(), entity.getCustomerId(), entity.getAppointmentId(), entity.getUserId(),
				entity.getReason(), entity.getCreatedAt());
	}
}
