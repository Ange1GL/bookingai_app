package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.application.query.ListRolesQuery;
import com.github.angellariosacosta.bookingapp.application.query.RoleSortField;
import com.github.angellariosacosta.bookingapp.application.query.SortDirection;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.PermissionEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RolePermissionEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.RoleMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.ActiveFilter;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryPermission;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryRole;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryRolePermission;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryUserRole;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaRoleRepositoryAdapter implements RoleRepository {

    private static final String TIE_BREAKER_PROPERTY = "id";

    private final JpaRepositoryRole jpaRepositoryRole;
    private final JpaRepositoryPermission jpaRepositoryPermission;
    private final JpaRepositoryRolePermission jpaRepositoryRolePermission;
    private final JpaRepositoryUserRole jpaRepositoryUserRole;
    private final RoleMapper roleMapper;

    @Override
    public Optional<Role> findByName(String name) {
        return jpaRepositoryRole.findByName(name)
                .map(roleMapper::toDomain);
    }

    @Override
    public List<Role> findAll() {
        return jpaRepositoryRole.findAll().stream()
                .map(roleMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Role> findById(Long id) {
        return jpaRepositoryRole.findByIdAndDeletedFalse(id).map(roleMapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepositoryRole.existsByName(name);
    }

    @Override
    public Role save(Role role) {
        if (role.id() == null) {
            return roleMapper.toDomain(jpaRepositoryRole.save(roleMapper.toEntity(role)));
        }
        // Se actualiza la fila existente (y no se reemplaza con un mapeo del dominio) para no perder `deleted` ni la auditoría.
        RoleEntity entity = loadOrFail(role.id());
        entity.setName(role.name());
        entity.setDescription(role.description());
        entity.setActive(role.active());
        return roleMapper.toDomain(jpaRepositoryRole.save(entity));
    }

    @Override
    public PageResult<Role> findPage(ListRolesQuery query) {
        Page<RoleEntity> page = jpaRepositoryRole.findPage(
                ActiveFilter.of(query.active()),
                LikePatterns.containsIgnoreCase(query.search()),
                PageRequest.of(query.page(), query.size(), toSort(query)));
        return new PageResult<>(
                page.getContent().stream().map(roleMapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Override
    public boolean isAssignedToUser(Long roleId) {
        return jpaRepositoryUserRole.existsActiveAssignment(roleId);
    }

    @Override
    public void replacePermissions(Long roleId, Set<Long> permissionIds) {
        RoleEntity role = loadOrFail(roleId);

        Map<Long, RolePermissionEntity> existing = new HashMap<>();
        for (RolePermissionEntity link : jpaRepositoryRolePermission.findByRoleId(roleId)) {
            existing.put(link.getPermissionId(), link);
            link.setActive(permissionIds.contains(link.getPermissionId()));
        }

        for (Long permissionId : permissionIds) {
            if (!existing.containsKey(permissionId)) {
                PermissionEntity permission = jpaRepositoryPermission.findById(permissionId)
                        .orElseThrow(() -> new IllegalStateException("Permission not found: " + permissionId));
                RolePermissionEntity created = jpaRepositoryRolePermission.save(RolePermissionEntity.builder()
                        .roleId(roleId)
                        .permissionId(permissionId)
                        .role(role)
                        .permission(permission)
                        .active(true)
                        .build());
                // Mantiene coherente la colección ya cargada en este contexto de persistencia.
                role.getRolePermissions().add(created);
            }
        }
        jpaRepositoryRolePermission.flush();
    }

    @Override
    public void markDeleted(Long id) {
        RoleEntity entity = loadOrFail(id);
        entity.markAsDeleted();
        entity.setActive(false);
        jpaRepositoryRole.save(entity);
    }

    private RoleEntity loadOrFail(Long id) {
        return jpaRepositoryRole.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + id));
    }

    // El id como desempate hace estable el orden entre páginas cuando el campo principal se repite.
    private Sort toSort(ListRolesQuery query) {
        Sort.Direction direction = query.direction() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, toProperty(query.sortBy())).and(Sort.by(direction, TIE_BREAKER_PROPERTY));
    }

    private String toProperty(RoleSortField field) {
        return switch (field) {
            case NAME -> "name";
            case CREATED_AT -> "createdAt";
        };
    }
}
