package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.adapter;

import com.github.angellariosacosta.bookingapp.application.port.out.UserRepository;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.UserRoleEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper.UserMapper;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryUser;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.repository.JpaRepositoryUserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

    private final JpaRepositoryUser jpaRepositoryUser;
    private final JpaRepositoryUserRole jpaRepositoryUserRole;
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
        UserEntity saved = jpaRepositoryUser.save(entity);

        for (Role role : user.getRoles()) {
            if (!jpaRepositoryUserRole.existsByUserIdAndRoleId(saved.getId(), role.id())) {
                jpaRepositoryUserRole.save(
                        UserRoleEntity.builder()
                                .userId(saved.getId())
                                .roleId(role.id())
                                .build()
                );
            }
        }

        return userMapper.toDomain(jpaRepositoryUser.findById(saved.getId()).orElseThrow());
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepositoryUser.existsByEmail(email);
    }
}
