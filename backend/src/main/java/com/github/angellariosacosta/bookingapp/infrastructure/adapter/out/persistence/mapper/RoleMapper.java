package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import com.github.angellariosacosta.bookingapp.domain.model.Permission;
import com.github.angellariosacosta.bookingapp.domain.model.Role;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.PermissionEntity;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    Permission toDomain(PermissionEntity entity);

    PermissionEntity toEntity(Permission permission);

    @Mapping(source = "permissionEntities", target = "permissions")
    Role toDomain(RoleEntity entity);

    @Mapping(target = "rolePermissions", ignore = true)
    RoleEntity toEntity(Role role);
}
