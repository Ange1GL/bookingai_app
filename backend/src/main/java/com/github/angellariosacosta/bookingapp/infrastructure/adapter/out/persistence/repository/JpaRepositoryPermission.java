package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.PermissionEntity;

public interface JpaRepositoryPermission extends JpaRepository<PermissionEntity, Long> {

    Optional<PermissionEntity> findByName(String name);

    Optional<PermissionEntity> findByIdAndDeletedFalse(Long id);

    List<PermissionEntity> findAllByIdInAndDeletedFalse(Collection<Long> ids);

    // Incluye eliminados a propósito: uk_permissions_name aplica también a las filas dadas de baja.
    boolean existsByName(String name);

    // El patrón llega ya en minúsculas, con '%', '_' y '\' escapados y "%" si no hay filtro,
    // así no hay parámetros null que Postgres no pueda tipar.
    @Query("""
            select p from PermissionEntity p
            where p.deleted = false
              and (:activeFilter = 0
                   or (:activeFilter = 1 and p.active = true)
                   or (:activeFilter = 2 and p.active = false))
              and (lower(p.name) like :pattern escape '\\'
                   or lower(coalesce(p.description, '')) like :pattern escape '\\')
            """)
    Page<PermissionEntity> findPage(
            @Param("activeFilter") int activeFilter,
            @Param("pattern") String pattern,
            Pageable pageable);
}
