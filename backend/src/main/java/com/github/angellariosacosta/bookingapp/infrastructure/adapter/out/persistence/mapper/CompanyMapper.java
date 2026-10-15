package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.result.CompanySearchResult;
import com.github.angellariosacosta.bookingapp.domain.model.Company;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.CompanyEntity;

@Component
public class CompanyMapper {

	public CompanyEntity toEntity(Company domain) {
		CompanyEntity entity = new CompanyEntity();
		entity.setUserId(domain.getUserId());
		entity.setName(domain.getName());
		return entity;
	}

	public Company toDomain(CompanyEntity entity) {
		return new Company(entity.getUserId(), entity.getName());
	}

	// Requiere el dueño cargado (las consultas de búsqueda usan EntityGraph).
	public CompanySearchResult toSearchResult(CompanyEntity entity) {
		return new CompanySearchResult(entity.getUserId(), entity.getName(), entity.getUser().getName());
	}
}
