package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.UserEntity;

public interface JpaRepositoryUser extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByIdAndDeletedFalse(Long id);

    Optional<UserEntity> findByEmailAndDeletedFalse(String email);

    Optional<UserEntity> findByUsernameAndDeletedFalse(String username);

    // Las comprobaciones de existencia incluyen eliminados a propósito: los UNIQUE aplican también a esas filas.
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    // El patrón llega ya en minúsculas, con '%', '_' y '\' escapados y "%" si no hay filtro. roleFilter 0 = sin
    // filtro por rol. Ambos son valores no nulos para que Postgres pueda tipar los parámetros.
    @Query("""
            select u from UserEntity u
            where u.deleted = false
              and (:activeFilter = 0
                   or (:activeFilter = 1 and u.active = true)
                   or (:activeFilter = 2 and u.active = false))
              and (:roleFilter = 0
                   or exists (select 1 from UserRoleEntity ur
                              where ur.userId = u.id and ur.roleId = :roleFilter and ur.active = true))
              and (lower(u.username) like :pattern escape '\\'
                   or lower(u.email) like :pattern escape '\\'
                   or lower(coalesce(u.name, '')) like :pattern escape '\\'
                   or coalesce(u.phone, '') like :pattern escape '\\')
            """)
    Page<UserEntity> findPage(
            @Param("activeFilter") int activeFilter,
            @Param("roleFilter") long roleFilter,
            @Param("pattern") String pattern,
            Pageable pageable);

    @Query("""
            select count(distinct u.id) from UserEntity u
              join u.userRoles ur
              join ur.role r
            where u.deleted = false and u.active = true
              and ur.active = true
              and r.name = :roleName and r.active = true and r.deleted = false
              and u.id <> :excludedUserId
            """)
    long countActiveWithRole(@Param("roleName") String roleName, @Param("excludedUserId") Long excludedUserId);
}
