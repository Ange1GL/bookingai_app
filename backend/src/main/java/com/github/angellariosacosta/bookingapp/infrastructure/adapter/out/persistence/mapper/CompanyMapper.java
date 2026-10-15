package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.domain.model.Company;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.CompanyEntity;

@Component
public class CompanyMapper {

	public CompanyEntity toEntity(Company domain) {
		CompanyEntity entity = new CompanyEntity();
		entity.setId(domain.getId());
		entity.setName(domain.getName());
		entity.setUserId(domain.getUserId());
		return entity;
	}

	public Company toDomain(CompanyEntity entity) {
		return new Company(entity.getId(), entity.getName(), entity.getUserId());
	}
}
