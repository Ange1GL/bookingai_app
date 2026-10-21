package com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto;

import java.util.List;

import com.github.angellariosacosta.bookingapp.application.result.PageResult;

/** Página de resultados de los listados administrativos. */
public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages
) {

	public static <T> PageResponse<T> of(PageResult<T> page) {
		return new PageResponse<>(page.content(), page.page(), page.size(), page.totalElements(), page.totalPages());
	}
}
