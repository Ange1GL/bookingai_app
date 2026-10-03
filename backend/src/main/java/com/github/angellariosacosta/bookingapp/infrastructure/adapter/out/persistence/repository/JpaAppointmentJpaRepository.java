package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.AppointmentEntity;

@Repository
public interface JpaAppointmentJpaRepository extends JpaRepository<AppointmentEntity, Long> {

	// Scoping por userId a nivel de query: un id que existe pero pertenece a otro
	// usuario simplemente no aparece, en vez de tener que compararlo manualmente
	// después de un findById sin filtrar (mismo patrón que CustomerJpaRepository).
	Optional<AppointmentEntity> findByIdAndUserId(Long id, Long userId);

	// Verifica si existe alguna cita que se solape con el intervalo [startTime, endTime].
	// La condición (startTime < :endTime AND endTime > :startTime) es el algoritmo clásico
	// de detección de solapamiento de intervalos: cubre todos los casos (parcial izquierdo,
	// parcial derecho, contenido, idéntico) con solo dos comparaciones, sin necesidad de OR.
	// Se excluyen las citas canceladas (statusId = 2) para no bloquear slots por citas inactivas.
	// Retorna boolean directamente: la BD detiene el scan al encontrar la primera coincidencia
	// (semántica de EXISTS), evitando traer entidades completas a memoria solo para .isEmpty().
	@Query("""
			SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
			FROM AppointmentEntity a
			WHERE 
			a.userId = :userId				
			AND a.startTime < :endTime
		 	AND a.endTime > :startTime
			AND a.statusId <> :cancelledStatusId
			""")
	boolean isOverlapping(
			@Param("userId") Long userId,
			@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime,
			@Param("cancelledStatusId") Integer cancelledStatusId
	);

	// Igual que isOverlapping, pero excluyendo la propia cita del chequeo.
	// Necesario para reagendar: al momento de validar el nuevo horario, la cita
	// todavía existe en la BD con su horario ANTERIOR, así que sin este exclude
	// cualquier reagendado que toque su propio slot actual se detectaría como
	// solapado consigo mismo (falso positivo).
	@Query("""
			SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
			FROM AppointmentEntity a
			WHERE 
			a.userId = :userId 
			  AND	a.startTime < :endTime
			  AND a.endTime > :startTime
			  AND a.statusId <> :cancelledStatusId
			  AND a.id <> :excludeId
			""")
	boolean isOverlappingExcludingId(
			@Param("userId")  Long userId,
			@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime,
			@Param("excludeId") Long excludeId,
			@Param("cancelledStatusId") Integer cancelledStatusId
	);

	// Spring Data deriva el SQL completo del nombre del método:
	// findBy → SELECT, CustomerIdAnd → WHERE customer_id = ?, UserIdAnd → AND user_id = ?,
	// StatusIdNot → AND status_id <> ?, OrderByStartTimeAsc → ORDER BY start_time ASC.
	// El userId scopea el resultado al dueño actual: sin él, cualquiera que conozca un
	// customerId ajeno podría listar sus citas.
	// El ORDER BY lo resuelve la BD (no en memoria) y la query se compila como prepared statement
	// en el arranque de la aplicación, no en cada llamada.
	List<AppointmentEntity> findByCustomerIdAndUserIdAndStatusIdNotOrderByStartTimeAsc(
			Long customerId, Long userId, Integer statusId);

	// Devuelve todas las citas activas que tocan el rango [from, to], aunque sea parcialmente.
	// La condición (startTime <= :to AND endTime >= :from) es la inversa del solapamiento:
	// trae citas que empiezan antes o en el límite superior Y terminan después o en el límite inferior.
	// JOIN FETCH a.customer carga el customer en la misma SELECT (un solo JOIN),
	// eliminando el problema N+1 que ocurriría si se accediera a customer.getX() después
	// con FetchType.LAZY (cada acceso dispararía una query adicional por cada cita).
	// Se excluyen canceladas (statusId <> 2) para no mostrar slots ocupados por citas inactivas.
	@Query("""
			SELECT a FROM AppointmentEntity a
			JOIN FETCH a.customer
			WHERE a.userId = :userId
			AND a.startTime <= :to
		  	AND a.endTime >= :from
		  	AND a.statusId <> :cancelledStatusId
			""")
	List<AppointmentEntity> findByTimeSlot(
			@Param("userId") Long userId,
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to,
			@Param("cancelledStatusId") Integer cancelledStatusId
	);
}
