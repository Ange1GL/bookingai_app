package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.PermissionRepositoryPort;
import com.github.angellariosacosta.bookingapp.application.query.ListPermissionsQuery;
import com.github.angellariosacosta.bookingapp.application.query.PermissionSortField;
import com.github.angellariosacosta.bookingapp.application.query.SortDirection;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Permission;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.PermissionEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.PermissionMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryPermission;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryRolePermission;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaPermissionRepositoryAdapter implements PermissionRepositoryPort {

	private static final String TIE_BREAKER_PROPERTY = "id";

	private final JpaRepositoryPermission jpaRepository;
	private final JpaRepositoryRolePermission rolePermissionRepository;
	private final PermissionMapper mapper;

	@Override
	public Optional<Permission> findById(Long id) {
		return jpaRepository.findByIdAndDeletedFalse(id).map(mapper::toDomain);
	}

	@Override
	public boolean existsByName(String name) {
		return jpaRepository.existsByName(name);
	}

	@Override
	public Permission save(Permission permission) {
		if (permission.id() == null) {
			return mapper.toDomain(jpaRepository.save(mapper.toEntity(permission)));
		}
		// Se actualiza la fila existente (y no se reemplaza con un mapeo del dominio) para no perder `deleted` ni la auditoría.
		PermissionEntity entity = jpaRepository.findByIdAndDeletedFalse(permission.id())
				.orElseThrow(() -> new IllegalStateException("Permission not found: " + permission.id()));
		entity.setDescription(permission.description());
		entity.setActive(permission.active());
		return mapper.toDomain(jpaRepository.save(entity));
	}

	@Override
	public PageResult<Permission> findPage(ListPermissionsQuery query) {
		Page<PermissionEntity> page = jpaRepository.findPage(
				toActiveFilter(query.active()),
				LikePatterns.containsIgnoreCase(query.search()),
				PageRequest.of(query.page(), query.size(), toSort(query)));
		return new PageResult<>(
				page.getContent().stream().map(mapper::toDomain).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}

	@Override
	public boolean isAssignedToRole(Long permissionId) {
		return rolePermissionRepository.existsActiveAssignment(permissionId);
	}

	@Override
	public void markDeleted(Long id) {
		PermissionEntity entity = jpaRepository.findByIdAndDeletedFalse(id)
				.orElseThrow(() -> new IllegalStateException("Permission not found: " + id));
		entity.markAsDeleted();
		entity.setActive(false);
		jpaRepository.save(entity);
	}

	private int toActiveFilter(Boolean active) {
		if (active == null) {
			return JpaRepositoryPermission.ACTIVE_FILTER_ANY;
		}
		return active
				? JpaRepositoryPermission.ACTIVE_FILTER_ONLY_ACTIVE
				: JpaRepositoryPermission.ACTIVE_FILTER_ONLY_INACTIVE;
	}

	// El id como desempate hace estable el orden entre páginas cuando el campo principal se repite.
	private Sort toSort(ListPermissionsQuery query) {
		Sort.Direction direction = query.direction() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
		return Sort.by(direction, toProperty(query.sortBy())).and(Sort.by(direction, TIE_BREAKER_PROPERTY));
	}

	private String toProperty(PermissionSortField field) {
		return switch (field) {
			case NAME -> "name";
			case CREATED_AT -> "createdAt";
		};
	}
}
