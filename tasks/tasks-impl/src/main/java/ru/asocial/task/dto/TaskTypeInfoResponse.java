package ru.asocial.task.dto;

public record TaskTypeInfoResponse(
		String code,
		String name,
		String description,
		String parametersDescription,
		String example,
		boolean requiresParameters) {
}
