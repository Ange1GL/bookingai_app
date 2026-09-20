package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import com.github.angellariosacosta.bookingapp.domain.model.RevokedAccessToken;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.RevokedAccessTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RevokedAccessTokenMapper {

    RevokedAccessToken toDomain(RevokedAccessTokenEntity entity);

    RevokedAccessTokenEntity toEntity(RevokedAccessToken domain);
}
