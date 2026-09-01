package ru.asocial.scheduler.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;

import ru.asocial.task.model.TaskType;

public record ScheduleCreateRequest(
		TaskType taskType,
		@JsonAlias("formula") String parameters,
		Instant nextRunAt,
		Long repeatIntervalMinutes) {
}
