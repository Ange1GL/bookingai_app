package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRepositoryRolePermission extends JpaRepository<RolePermissionEntity, Long> {

    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
