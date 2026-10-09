package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.angellariosacosta.bookingapp.application.command.DeletePriceCatalogCommand;
import com.github.angellariosacosta.bookingapp.application.port.in.CreatePriceCatalogUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.DeletePriceCatalogUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.ListPriceCatalogUseCase;
import com.github.angellariosacosta.bookingapp.application.port.in.UpdatePriceCatalogUseCase;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.annotation.CurrentUserId;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PriceCatalogRequest;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.PriceCatalogResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.mapper.PriceCatalogRestMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/price-catalog")
@RequiredArgsConstructor
public class PriceCatalogController {

	private final ListPriceCatalogUseCase listPriceCatalog;
	private final CreatePriceCatalogUseCase createPriceCatalog;
	private final UpdatePriceCatalogUseCase updatePriceCatalog;
	private final DeletePriceCatalogUseCase deletePriceCatalog;
	private final PriceCatalogRestMapper priceCatalogRestMapper;

	@GetMapping
	public ResponseEntity<List<PriceCatalogResponse>> list(@CurrentUserId Long userId) {
		return ResponseEntity.ok(priceCatalogRestMapper.toResponseList(listPriceCatalog.list(userId)));
	}

	@PostMapping
	public ResponseEntity<PriceCatalogResponse> create(
			@Valid @RequestBody PriceCatalogRequest request,
			@CurrentUserId Long userId) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(priceCatalogRestMapper.toResponse(createPriceCatalog.create(priceCatalogRestMapper.toCreateCommand(request, userId))));
	}

	@PutMapping("/{id}")
	public ResponseEntity<PriceCatalogResponse> update(
			@PathVariable Integer id,
			@Valid @RequestBody PriceCatalogRequest request,
			@CurrentUserId Long userId) {
		return ResponseEntity.ok(
				priceCatalogRestMapper.toResponse(updatePriceCatalog.update(priceCatalogRestMapper.toUpdateCommand(id, request, userId))));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Integer id, @CurrentUserId Long userId) {
		deletePriceCatalog.delete(new DeletePriceCatalogCommand(id, userId));
		return ResponseEntity.noContent().build();
	}
}
