package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.AppointmentEntity;

@Repository
public interface AppointmentRepositoryJpa extends JpaRepository<AppointmentEntity, Long> {

	@Query("SELECT COUNT(a) > 0 FROM AppointmentEntity a WHERE a.startTime < :endTime AND a.endTime > :startTime")
	boolean existsOverlapping(
			@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime
	);
}
