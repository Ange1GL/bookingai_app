package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.domain.model.StatusAppointment;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.AppointmentEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppointmentMapper {


	private final CustomerMapper customerMapper;


	public AppointmentEntity toEntity(Appointment domain) {
		AppointmentEntity entity = new AppointmentEntity();
		entity.setStartTime(domain.getStartTime());
		entity.setEndTime(domain.getEndTime());
		entity.setCustomerId(domain.getCustomer().getId());
		entity.setStatusId(domain.getStatus().getId());
		entity.setUserId(domain.getUserId());
		return entity;
	}


	public Appointment toDomain(AppointmentEntity entity) {
		StatusAppointment status = StatusAppointment.fromId(entity.getStatusId());
		Customer customer = customerMapper.toDomain(entity.getCustomer());
		Appointment domain = new Appointment(
				entity.getStartTime(),
				entity.getEndTime(),
				customer,
				status,
				entity.getUserId()
		);
		domain.setId(entity.getId());
		return domain;
	}

}
