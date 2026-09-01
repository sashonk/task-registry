package ru.asocial.scheduler.dto;

import java.time.Instant;

import ru.asocial.task.model.TaskType;

public record ScheduleResponse(
		Long id,
		TaskType taskType,
		String formula,
		Instant nextRunAt,
		Long repeatIntervalMinutes,
		boolean enabled,
		Instant lastTriggeredAt,
		Instant createdAt) {
}
