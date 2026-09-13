package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;


import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaRepositoryPermission extends JpaRepository<PermissionEntity, Long> {
    Optional<PermissionEntity> findByName(String name);
}
