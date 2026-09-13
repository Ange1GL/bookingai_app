package com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.domain.model.Customer;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.out.persistence.entity.CustomerEntity;

@Component
public class CustomerMapper {
	
	
	public CustomerEntity toEntity(Customer customer) {
		CustomerEntity entity = new CustomerEntity();
		entity.setFullName(customer.getName());
		entity.setPhone(customer.getPhone());
		return entity;
	}
	
	
	public Customer toDomain(CustomerEntity entity) {
		return Customer
							.builder()
							.name(entity.getFullName())
							.phone(entity.getPhone())
							.id(entity.getId())
							.build();
		

	}
}
