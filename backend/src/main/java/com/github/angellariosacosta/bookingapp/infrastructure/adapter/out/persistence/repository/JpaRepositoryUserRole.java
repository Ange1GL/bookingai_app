package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.UserRoleEntity;

public interface JpaRepositoryUserRole extends JpaRepository<UserRoleEntity, Long> {

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    // "En uso" = vínculo activo con un usuario que no ha sido eliminado.
    @Query("""
            select count(ur) > 0 from UserRoleEntity ur
            where ur.roleId = :roleId and ur.active = true and ur.user.deleted = false
            """)
    boolean existsActiveAssignment(@Param("roleId") Long roleId);
}
