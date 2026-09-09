package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepository;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.AppointmentEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.AppointmentMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.AppointmentRepositoryJpa;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppointmentRepositoryAdapter implements AppointmentRepository {

	private final AppointmentRepositoryJpa jpaRepository;
	private final AppointmentMapper mapper;

	@Override
	public Appointment save(Appointment appointment) {
		AppointmentEntity entity = mapper.toEntity(appointment);
		AppointmentEntity saved = jpaRepository.save(entity);
		appointment.setId(saved.getId());
		return appointment;
	}

	@Override
	public boolean isOverlapping(LocalDateTime startTime, LocalDateTime endTime) {
		return jpaRepository.existsOverlapping(startTime, endTime);
	}
}
