package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import com.github.angellariosacosta.bookingapp.domain.model.RefreshToken;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RefreshTokenEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RefreshTokenMapper {

    RefreshToken toDomain(RefreshTokenEntity entity);

    @Mapping(target = "user", ignore = true)
    RefreshTokenEntity toEntity(RefreshToken domain);
}
