package ru.asocial.task.dto;

import java.time.LocalDateTime;

import ru.asocial.task.model.TaskStatus;
import ru.asocial.task.model.TaskType;

public record TaskResponse(
		Long id,
		TaskType type,
		String executorName,
		LocalDateTime createdAt,
		LocalDateTime startedAt,
		LocalDateTime completedAt,
		TaskStatus status,
		Long completionPercent,
		String formula) {
}
