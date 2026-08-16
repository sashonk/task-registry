package ru.asocial.task.dto;

import java.time.LocalDateTime;

public record TaskLogTableRowResponse(
		Long id,
		Long taskId,
		String taskName,
		String message,
		LocalDateTime createdAt) {
}
