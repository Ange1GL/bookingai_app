package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
	public Optional<Appointment> findById(Long id, Long userId) {
		return jpaRepository.findByIdAndUserId(id, userId).map(mapper::toDomain);
	}

	@Override
	public boolean isOverlapping(LocalDateTime startTime, LocalDateTime endTime) {
		return jpaRepository.existsOverlapping(startTime, endTime);
	}

	@Override
	public boolean isOverlapping(LocalDateTime startTime, LocalDateTime endTime, Long excludeAppointmentId) {
		return jpaRepository.existsOverlappingExcludingId(startTime, endTime, excludeAppointmentId);
	}

	@Override
	public List<Appointment> findByCustomerId(Long customerId, Long userId) {
		return jpaRepository.findByCustomerIdAndUserIdAndStatusIdNotOrderByStartTimeAsc(customerId, userId, 2)
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
	public Appointment updateStatus(Long id, Long userId, StatusAppointment newStatus) {
		AppointmentEntity entity = findEntityByIdAndUserId(id, userId);
		entity.setStatusId(newStatus.getId());
		return mapper.toDomain(jpaRepository.save(entity));
	}

	@Override
	public Appointment updateTimeSlot(Long id, Long userId, LocalDateTime newStart, LocalDateTime newEnd) {
		AppointmentEntity entity = findEntityByIdAndUserId(id, userId);
		entity.setStartTime(newStart);
		entity.setEndTime(newEnd);
		return mapper.toDomain(jpaRepository.save(entity));
	}

	private AppointmentEntity findEntityByIdAndUserId(Long id, Long userId) {
		return jpaRepository.findByIdAndUserId(id, userId)
				.orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with id: " + id));
	}
}
