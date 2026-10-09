package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.github.angellariosacosta.bookingapp.application.command.CreatePriceCatalogCommand;
import com.github.angellariosacosta.bookingapp.application.command.UpdatePriceCatalogCommand;
import com.github.angellariosacosta.bookingapp.domain.model.PriceCatalog;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PriceCatalogRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PriceCatalogResponse;

@Component
public class PriceCatalogRestMapper {

	public CreatePriceCatalogCommand toCreateCommand(PriceCatalogRequest request, Long userId) {
		return new CreatePriceCatalogCommand(request.label(), request.price(), userId);
	}

	public UpdatePriceCatalogCommand toUpdateCommand(Integer id, PriceCatalogRequest request, Long userId) {
		return new UpdatePriceCatalogCommand(id, request.label(), request.price(), userId);
	}

	public PriceCatalogResponse toResponse(PriceCatalog priceCatalog) {
		return new PriceCatalogResponse(
				priceCatalog.getId(),
				priceCatalog.getLabel(),
				priceCatalog.getPrice()
		);
	}

	public List<PriceCatalogResponse> toResponseList(List<PriceCatalog> priceCatalogs) {
		return priceCatalogs.stream().map(this::toResponse).toList();
	}
}
