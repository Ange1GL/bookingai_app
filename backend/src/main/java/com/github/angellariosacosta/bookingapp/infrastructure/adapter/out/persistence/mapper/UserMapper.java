package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import com.github.angellariosacosta.bookingapp.domain.model.User;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = RoleMapper.class)
public interface UserMapper {

    @Mapping(source = "roleEntities", target = "roles")
    User toDomain(UserEntity entity);

    @Mapping(target = "userRoles", ignore = true)
    UserEntity toEntity(User user);

    // Copia los datos del dominio sobre la fila existente; los vínculos de rol se gestionan aparte en el adaptador.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    void updateEntity(User user, @MappingTarget UserEntity entity);
}
