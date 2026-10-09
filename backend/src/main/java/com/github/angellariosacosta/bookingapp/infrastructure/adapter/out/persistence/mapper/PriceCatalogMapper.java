package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.PriceCatalogEntity;

@Component
public class PriceCatalogMapper {

	public PriceCatalogEntity toEntity(PriceCatalog domain) {
		PriceCatalogEntity entity = new PriceCatalogEntity();
		entity.setId(domain.getId());
		entity.setLabel(domain.getLabel());
		entity.setPrice(domain.getPrice());
		entity.setUserId(domain.getUserId());
		entity.setActive(domain.isActive());
		return entity;
	}

	public PriceCatalog toDomain(PriceCatalogEntity entity) {
		return new PriceCatalog(entity.getId(), entity.getPrice(), entity.getLabel(), entity.getUserId(), entity.isActive());
	}
}
