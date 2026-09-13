package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;


import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaRepositoryRole extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByName(String name);
}
