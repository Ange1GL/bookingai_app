package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.AppointmentEntity;

@Repository
public interface AppointmentRepositoryJpa extends JpaRepository<AppointmentEntity, Long> {

	@Query("""
			SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
			FROM AppointmentEntity a
			WHERE a.startTime < :endTime
			  AND a.endTime > :startTime
			  AND a.statusId <> 2
			""")
	boolean existsOverlapping(
			@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime
	);

	List<AppointmentEntity> findByCustomerIdAndStatusIdNotOrderByStartTimeAsc(Long customerId, Integer statusId);

	@Query("""
			SELECT a FROM AppointmentEntity a
			JOIN FETCH a.customer
			WHERE a.startTime <= :to
			  AND a.endTime >= :from
			  AND a.statusId <> 2
			""")
	List<AppointmentEntity> findByTimeSlot(
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to
	);
}
