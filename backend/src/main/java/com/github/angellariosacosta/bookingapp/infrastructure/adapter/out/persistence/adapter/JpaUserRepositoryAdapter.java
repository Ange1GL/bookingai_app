package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.application.query.ListUsersQuery;
import com.github.angellariosacosta.bookingapp.application.query.SortDirection;
import com.github.angellariosacosta.bookingapp.application.query.UserSortField;
import com.github.angellariosacosta.bookingapp.application.result.PageResult;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.UserRoleEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.UserMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.ActiveFilter;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryRole;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryUser;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryUserRole;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

    private static final String TIE_BREAKER_PROPERTY = "id";
    private static final long NO_ROLE_FILTER = 0L;

    private final JpaRepositoryUser jpaRepositoryUser;
    private final JpaRepositoryUserRole jpaRepositoryUserRole;
    private final JpaRepositoryRole jpaRepositoryRole;
    private final UserMapper userMapper;

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepositoryUser.findByIdAndDeletedFalse(id)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepositoryUser.findByEmailAndDeletedFalse(email)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepositoryUser.findByUsernameAndDeletedFalse(username)
                .map(userMapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity saved = jpaRepositoryUser.save(toManagedEntity(user));

        for (Role role : user.getRoles()) {
            ensureActiveLink(saved, role.id());
        }

        return userMapper.toDomain(jpaRepositoryUser.findById(saved.getId()).orElseThrow());
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepositoryUser.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepositoryUser.existsByUsername(username);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return jpaRepositoryUser.existsByPhone(phone);
    }

    @Override
    public PageResult<User> findPage(ListUsersQuery query) {
        Page<UserEntity> page = jpaRepositoryUser.findPage(
                ActiveFilter.of(query.active()),
                query.roleId() == null ? NO_ROLE_FILTER : query.roleId(),
                LikePatterns.containsIgnoreCase(query.search()),
                PageRequest.of(query.page(), query.size(), toSort(query)));
        return new PageResult<>(
                page.getContent().stream().map(userMapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Override
    public long countActiveUsersWithRole(String roleName, Long excludedUserId) {
        return jpaRepositoryUser.countActiveWithRole(roleName, excludedUserId);
    }

    @Override
    public void replaceRoles(Long userId, Set<Long> roleIds) {
        UserEntity user = loadOrFail(userId);
        user.getUserRoles().forEach(link -> link.setActive(roleIds.contains(link.getRoleId())));
        roleIds.forEach(roleId -> ensureActiveLink(user, roleId));
        jpaRepositoryUserRole.flush();
    }

    @Override
    public void markDeleted(Long id) {
        UserEntity entity = loadOrFail(id);
        entity.markAsDeleted();
        entity.setActive(false);
        jpaRepositoryUser.save(entity);
    }

    // Alta con el mapeo del dominio; si ya existe se actualiza la fila (no se reemplaza) para conservar `deleted` y la auditoría.
    private UserEntity toManagedEntity(User user) {
        if (user.getId() == null) {
            return userMapper.toEntity(user);
        }
        UserEntity entity = loadOrFail(user.getId());
        userMapper.updateEntity(user, entity);
        return entity;
    }

    // Reactiva el vínculo si existía revocado; si no, lo crea. Mantiene coherente la colección ya cargada.
    private void ensureActiveLink(UserEntity user, Long roleId) {
        Optional<UserRoleEntity> existing = user.getUserRoles().stream()
                .filter(link -> roleId.equals(link.getRoleId()))
                .findFirst();
        if (existing.isPresent()) {
            existing.get().setActive(true);
            return;
        }
        RoleEntity role = jpaRepositoryRole.findById(roleId)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleId));
        UserRoleEntity created = jpaRepositoryUserRole.save(UserRoleEntity.builder()
                .userId(user.getId())
                .roleId(roleId)
                .user(user)
                .role(role)
                .active(true)
                .build());
        user.getUserRoles().add(created);
    }

    private UserEntity loadOrFail(Long id) {
        return jpaRepositoryUser.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalStateException("User not found: " + id));
    }

    // El id como desempate hace estable el orden entre páginas cuando el campo principal se repite.
    private Sort toSort(ListUsersQuery query) {
        Sort.Direction direction = query.direction() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, toProperty(query.sortBy())).and(Sort.by(direction, TIE_BREAKER_PROPERTY));
    }

    private String toProperty(UserSortField field) {
        return switch (field) {
            case USERNAME -> "username";
            case NAME -> "name";
            case EMAIL -> "email";
            case CREATED_AT -> "createdAt";
        };
    }
}
