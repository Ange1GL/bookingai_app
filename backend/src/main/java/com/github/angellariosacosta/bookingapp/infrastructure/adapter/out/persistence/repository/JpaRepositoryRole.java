package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RoleEntity;

public interface JpaRepositoryRole extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByName(String name);

    Optional<RoleEntity> findByIdAndDeletedFalse(Long id);

    // Incluye eliminados a propósito: uk_roles_name aplica también a las filas dadas de baja.
    boolean existsByName(String name);

    // El patrón llega ya en minúsculas, con '%', '_' y '\' escapados y "%" si no hay filtro.
    @Query("""
            select r from RoleEntity r
            where r.deleted = false
              and (:activeFilter = 0
                   or (:activeFilter = 1 and r.active = true)
                   or (:activeFilter = 2 and r.active = false))
              and (lower(r.name) like :pattern escape '\\'
                   or lower(coalesce(r.description, '')) like :pattern escape '\\')
            """)
    Page<RoleEntity> findPage(
            @Param("activeFilter") int activeFilter,
            @Param("pattern") String pattern,
            Pageable pageable);
}
