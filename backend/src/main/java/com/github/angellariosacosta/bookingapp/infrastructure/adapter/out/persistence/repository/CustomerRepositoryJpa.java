package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.CustomerEntity;

public interface CustomerRepositoryJpa extends JpaRepository<CustomerEntity, Long> {
	Optional<CustomerEntity> findByPhone(String phone);
	List<CustomerEntity> findByFullNameContainingIgnoreCase(String name);
}
