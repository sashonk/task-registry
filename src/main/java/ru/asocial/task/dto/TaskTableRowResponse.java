package ru.asocial.task.dto;

import java.time.LocalDateTime;

import ru.asocial.task.model.TaskStatus;
import ru.asocial.task.service.task.TaskDisplayNames;

public record TaskTableRowResponse(
		Long id,
		String name,
		String executor,
		LocalDateTime launchDateTime,
		TaskStatus status,
		Long completionPercent) {

	public static TaskTableRowResponse from(TaskResponse task) {
		LocalDateTime launchDateTime = task.startedAt() != null ? task.startedAt() : task.createdAt();
		String executorName = task.executorName() != null ? task.executorName() : "—";
		return new TaskTableRowResponse(
				task.id(),
				formatName(task),
				executorName,
				launchDateTime,
				task.status(),
				task.completionPercent());
	}

	private static String formatName(TaskResponse task) {
		return TaskDisplayNames.format(task.type(), task.formula());
	}
}
