package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;


import com.github.angellariosacosta.bookingapp.application.port.out.RoleRepository;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.RoleMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaRoleRepositoryAdapter implements RoleRepository {

    private final JpaRepositoryRole jpaRepositoryRole;
    private final RoleMapper roleMapper;

    @Override
    public Optional<Role> findByName(String name) {
        return jpaRepositoryRole.findByName(name)
                .map(roleMapper::toDomain);
    }

    @Override
    public Role save(Role role) {
        return roleMapper.toDomain(
                jpaRepositoryRole.save(roleMapper.toEntity(role))
        );
    }

    @Override
    public List<Role> findAll() {
        return jpaRepositoryRole.findAll().stream()
                .map(roleMapper::toDomain)
                .toList();
    }
}
