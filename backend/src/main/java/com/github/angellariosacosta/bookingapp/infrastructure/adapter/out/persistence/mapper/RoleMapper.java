package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import com.github.angellariosacosta.bookingapp.domain.model.Permission;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.PermissionEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    Permission toDomain(PermissionEntity entity);

    PermissionEntity toEntity(Permission permission);

    Role toDomain(RoleEntity entity);

    RoleEntity toEntity(Role role);
}
