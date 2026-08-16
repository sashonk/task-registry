package ru.asocial.task.dto;

public record ExecutorTableRowResponse(
		Long id,
		String fullName,
		String status,
		long successfulTasksCount,
		long errorTasksCount,
		long abortedTasksCount) {

	public static ExecutorTableRowResponse from(ExecutorResponse executor) {
		return new ExecutorTableRowResponse(
				executor.id(),
				executor.name(),
				formatStatus(executor),
				executor.successfulTasksCount(),
				executor.errorTasksCount(),
				executor.abortedTasksCount());
	}

	private static String formatStatus(ExecutorResponse executor) {
		return switch (executor.state()) {
			case WORKING -> "В работе";
			case IDLE -> "Свободен";
		};
	}
}
