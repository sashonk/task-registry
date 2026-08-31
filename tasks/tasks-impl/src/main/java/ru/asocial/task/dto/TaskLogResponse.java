package ru.asocial.task.dto;

import java.time.LocalDateTime;

public record TaskLogResponse(
		Long id,
		String message,
		LocalDateTime createdAt) {
}
