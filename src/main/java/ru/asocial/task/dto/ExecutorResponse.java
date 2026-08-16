package ru.asocial.task.dto;

import ru.asocial.task.model.ExecutorState;

public record ExecutorResponse(
		Long id,
		String name,
		ExecutorState state,
		long successfulTasksCount,
		long errorTasksCount,
		long abortedTasksCount) {
}
