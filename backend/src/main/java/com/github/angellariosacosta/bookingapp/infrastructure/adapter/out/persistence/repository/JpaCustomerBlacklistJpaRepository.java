package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.CustomerBlacklistEntity;

public interface JpaCustomerBlacklistJpaRepository extends JpaRepository<CustomerBlacklistEntity, Long> {

	boolean existsByCustomerIdAndUserId(Long customerId, Long userId);

	@Modifying
	@Query("delete from CustomerBlacklistEntity b where b.customerId = :customerId and b.userId = :userId")
	int deleteByCustomerIdAndUserId(@Param("customerId") Long customerId, @Param("userId") Long userId);

	@Query("select b.customerId from CustomerBlacklistEntity b where b.userId = :userId and b.customerId in :customerIds")
	List<Long> findBlacklistedCustomerIds(
			@Param("userId") Long userId, @Param("customerIds") Collection<Long> customerIds);
}
