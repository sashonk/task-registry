package ru.asocial.task.dto;

import java.time.LocalDateTime;

import ru.asocial.task.model.TaskType;

public record ScheduleResponse(
		Long id,
		TaskType taskType,
		String formula,
		LocalDateTime nextRunAt,
		Long repeatIntervalMinutes,
		boolean enabled,
		LocalDateTime lastTriggeredAt,
		LocalDateTime createdAt) {
}
