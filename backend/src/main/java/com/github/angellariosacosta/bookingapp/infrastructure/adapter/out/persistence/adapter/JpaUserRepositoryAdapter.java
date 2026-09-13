package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;


import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.UserMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryRole;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

    private final JpaRepositoryUser jpaRepositoryUser;
    private final JpaRepositoryRole jpaRepositoryRole;
    private final UserMapper userMapper;

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepositoryUser.findById(id)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepositoryUser.findByEmail(email)
                .map(userMapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        entity.setRoles(resolveRoleEntities(user.getRoles()));
        return userMapper.toDomain(jpaRepositoryUser.save(entity));
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepositoryUser.existsByEmail(email);
    }

    private Set<RoleEntity> resolveRoleEntities(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) return new HashSet<>();

        Set<RoleEntity> resolved = new HashSet<>();
        for (Role role : roles) {
            RoleEntity roleEntity = jpaRepositoryRole.findById(role.id())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + role.name()));
            resolved.add(roleEntity);
        }
        return resolved;
    }
}
