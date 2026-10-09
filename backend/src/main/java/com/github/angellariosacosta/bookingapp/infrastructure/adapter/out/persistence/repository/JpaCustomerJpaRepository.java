package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.CustomerEntity;

public interface JpaCustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {
	Optional<CustomerEntity> findByIdAndUserId(Long id, Long userId);
	Optional<CustomerEntity> findByPhoneAndUserId(String phone, Long userId);
	List<CustomerEntity> findByFullNameContainingIgnoreCaseAndUserId(String name, Long userId);

	// Los patrones llegan ya construidos (en minúsculas el de nombre, "%" si no hay filtro) y con
	// '%', '_' y '\' escapados, así no hay parámetros null que Postgres no pueda tipar.
	@Query("""
			select c from CustomerEntity c
			where c.userId = :userId
			  and lower(c.fullName) like :namePattern escape '\\'
			  and c.phone like :phonePattern escape '\\'
			""")
	Page<CustomerEntity> findPage(
			@Param("userId") Long userId,
			@Param("namePattern") String namePattern,
			@Param("phonePattern") String phonePattern,
			Pageable pageable);
}
