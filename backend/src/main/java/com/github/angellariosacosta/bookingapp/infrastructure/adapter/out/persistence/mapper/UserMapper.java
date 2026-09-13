package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = RoleMapper.class)
public interface UserMapper {

    User toDomain(UserEntity entity);

    @Mapping(target = "roles", ignore = true)
    UserEntity toEntity(User user);
}
