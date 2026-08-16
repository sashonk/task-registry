package ru.asocial.task.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		int from,
		int to) {

	private static final int MAX_PAGE_SIZE = 100;

	public static int normalizePage(int page) {
		return Math.max(1, page);
	}

	public static int normalizeSize(int size) {
		return Math.min(MAX_PAGE_SIZE, Math.max(1, size));
	}

	public static <T> PageResponse<T> from(Page<T> page) {
		int fromIndex = page.getTotalElements() == 0 ? 0 : page.getNumber() * page.getSize() + 1;
		int toIndex = page.getTotalElements() == 0 ? 0 : fromIndex + page.getNumberOfElements() - 1;
		return new PageResponse<>(
				page.getContent(),
				page.getNumber() + 1,
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				fromIndex,
				toIndex);
	}
}
