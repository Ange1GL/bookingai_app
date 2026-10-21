package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;

import com.github.angellariosacosta.bookingapp.domain.model.Permission;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.PermissionEntity;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    Permission toDomain(PermissionEntity entity);

    PermissionEntity toEntity(Permission permission);
}
