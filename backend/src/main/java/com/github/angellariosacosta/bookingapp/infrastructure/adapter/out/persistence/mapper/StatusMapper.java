package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.domain.model.StatusAppointment;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.StatusAppointmentEntity;



@Component
public class StatusMapper {

	
	public StatusAppointmentEntity toEntity(StatusAppointment domain) {
		StatusAppointmentEntity entity = new StatusAppointmentEntity();
		entity.setId(domain.getId());
		entity.setNombre(domain.getName());
		return entity;
	}
	
	
	public StatusAppointment toDomain(StatusAppointmentEntity entity) {
		return StatusAppointment.fromId(entity.getId());
	}
}
