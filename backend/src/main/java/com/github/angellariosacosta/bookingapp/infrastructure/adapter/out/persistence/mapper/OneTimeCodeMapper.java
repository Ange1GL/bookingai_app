package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.domain.model.OneTimeCode;
import com.github.angellariosacosta.bookingapp.domain.model.OneTimeCodePurpose;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.OneTimeCodeEntity;

@Component
public class OneTimeCodeMapper {

    public OneTimeCode toDomain(OneTimeCodeEntity entity) {
        return OneTimeCode.reconstitute(
                entity.getId(),
                entity.getUserId(),
                OneTimeCodePurpose.fromId(entity.getPurposeId()),
                entity.getCodeHash(),
                entity.getExpiresAt(),
                entity.getFailedAttempts(),
                entity.getConsumedAt(),
                entity.getCreatedAt());
    }

    public OneTimeCodeEntity toNewEntity(OneTimeCode code) {
        OneTimeCodeEntity entity = new OneTimeCodeEntity();
        entity.setUserId(code.getUserId());
        entity.setPurposeId(code.getPurpose().getId());
        entity.setCodeHash(code.getCodeHash());
        entity.setExpiresAt(code.getExpiresAt());
        entity.setCreatedAt(code.getCreatedAt());
        copyMutableState(code, entity);
        return entity;
    }

    /** Solo cambia con el uso: intentos fallidos y consumo. El resto del código es inmutable. */
    public void copyMutableState(OneTimeCode code, OneTimeCodeEntity entity) {
        entity.setFailedAttempts((short) code.getFailedAttempts());
        entity.setConsumedAt(code.getConsumedAt());
    }
}
