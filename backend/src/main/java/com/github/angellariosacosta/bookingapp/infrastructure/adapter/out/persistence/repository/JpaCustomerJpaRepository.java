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
	// blacklistFilter es un entero (nunca null, por la misma razón): ANY no filtra, ONLY_BLACKLISTED
	// deja solo los que tienen fila en customer_blacklist y EXCLUDING_BLACKLISTED solo los que no.
	int BLACKLIST_FILTER_ANY = 0;
	int BLACKLIST_FILTER_ONLY_BLACKLISTED = 1;
	int BLACKLIST_FILTER_EXCLUDING_BLACKLISTED = 2;

	@Query("""
			select c from CustomerEntity c
			where c.userId = :userId
			  and lower(c.fullName) like :namePattern escape '\\'
			  and c.phone like :phonePattern escape '\\'
			  and (:blacklistFilter = 0
			       or :blacklistFilter = case
			            when exists (select 1 from CustomerBlacklistEntity b where b.customerId = c.id) then 1
			            else 2 end)
			""")
	Page<CustomerEntity> findPage(
			@Param("userId") Long userId,
			@Param("namePattern") String namePattern,
			@Param("phonePattern") String phonePattern,
			@Param("blacklistFilter") int blacklistFilter,
			Pageable pageable);
}
