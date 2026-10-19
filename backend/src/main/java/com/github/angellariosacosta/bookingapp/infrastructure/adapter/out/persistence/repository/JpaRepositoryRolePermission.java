package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RolePermissionEntity;

public interface JpaRepositoryRolePermission extends JpaRepository<RolePermissionEntity, Long> {

    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);

    // "En uso" = vínculo activo con un rol que no ha sido eliminado.
    @Query("""
            select count(rp) > 0 from RolePermissionEntity rp
            where rp.permissionId = :permissionId and rp.active = true and rp.role.deleted = false
            """)
    boolean existsActiveAssignment(@Param("permissionId") Long permissionId);
}
