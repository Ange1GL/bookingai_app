package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRepositoryUserRole extends JpaRepository<UserRoleEntity, Long> {

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);
}
