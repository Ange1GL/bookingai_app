package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepository;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.StatusAppointment;
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

	@Override
	public List<Appointment> findByCustomerId(Long customerId) {
		return jpaRepository.findByCustomerIdAndStatusIdNotOrderByStartTimeAsc(customerId, 2)
				.stream()
				.map(mapper::toDomain)
				.toList();
	}

	@Override
	public List<Appointment> findByTimeSlot(LocalDateTime from, LocalDateTime to) {
		return jpaRepository.findByTimeSlot(from, to)
				.stream()
				.map(mapper::toDomain)
				.toList();
	}

	@Override
	public Appointment updateStatus(Long id, StatusAppointment newStatus) {
		AppointmentEntity entity = findEntityById(id);
		entity.setStatusId(newStatus.getId());
		return mapper.toDomain(jpaRepository.save(entity));
	}

	@Override
	public Appointment updateTimeSlot(Long id, LocalDateTime newStart, LocalDateTime newEnd) {
		AppointmentEntity entity = findEntityById(id);
		entity.setStartTime(newStart);
		entity.setEndTime(newEnd);
		return mapper.toDomain(jpaRepository.save(entity));
	}

	private AppointmentEntity findEntityById(Long id) {
		return jpaRepository.findById(id)
				.orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with id: " + id));
	}
}
