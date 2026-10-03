package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.AppointmentRepositoryPort;
import com.github.angellariosacosta.bookingapp.domain.exception.AppointmentNotFoundException;
import com.github.angellariosacosta.bookingapp.domain.model.Appointment;
import com.github.angellariosacosta.bookingapp.domain.model.StatusAppointment;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.AppointmentEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.AppointmentMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaAppointmentJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaAppointmentRepositoryAdapter implements AppointmentRepositoryPort {

	private final JpaAppointmentJpaRepository jpaRepository;
	private final AppointmentMapper mapper;

	private static final Integer CANCELLED_STATUS_ID = StatusAppointment.CANCELLED.getId();

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
	public boolean isOverlapping(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
		return jpaRepository.isOverlapping(userId, startTime, endTime, CANCELLED_STATUS_ID);
	}

	@Override
	public boolean isOverlapping(Long userId, LocalDateTime startTime, LocalDateTime endTime, Long excludeAppointmentId) {
		return jpaRepository.isOverlappingExcludingId(userId, startTime, endTime, excludeAppointmentId, CANCELLED_STATUS_ID);
	}

	@Override
	public List<Appointment> findByCustomerId(Long customerId, Long userId) {
		return jpaRepository.findByCustomerIdAndUserIdAndStatusIdNotOrderByStartTimeAsc(customerId, userId, CANCELLED_STATUS_ID)
				.stream()
				.map(mapper::toDomain)
				.toList();
	}

	@Override
	public List<Appointment> findByTimeSlot(Long userId, LocalDateTime from, LocalDateTime to) {
		return jpaRepository.findByTimeSlot(userId, from, to, CANCELLED_STATUS_ID)
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
