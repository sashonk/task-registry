package ru.asocial.scheduler.validation;

import tools.jackson.databind.JsonNode;

import ru.asocial.scheduler.exception.BadRequestException;
import ru.asocial.task.model.TaskType;

public final class TaskParameterValidator {

	private TaskParameterValidator() {
	}

	public static void validate(TaskType type, String parameters) {
		switch (type) {
			case CALCULATION -> requireText(parameters, "Formula is required for CALCULATION task");
			case DELAY -> requirePositiveInt(parseJson(parameters), "durationSeconds", "durationSeconds is required for DELAY task");
			case RANDOM -> validateRandom(parseJson(parameters));
			case TEXT_TRANSFORM -> validateTextTransform(parseJson(parameters));
			case HASH -> validateHash(parseJson(parameters));
			case HTTP_REQUEST -> validateHttpRequest(parseJson(parameters));
			case FILE_CHECK -> validateFileCheck(parseJson(parameters));
			case SQL_QUERY -> validateSqlQuery(parseJson(parameters));
			case REPORT -> {
			}
			case CLEANUP -> validateCleanup(parseJson(parameters));
			case BATCH -> validateBatch(parseJson(parameters));
			case SIMULATION -> validateSimulation(parseJson(parameters));
			case SCRIPT -> {
				JsonNode node = parseJson(parameters);
				requireField(node, "script");
			}
		}
	}

	public static boolean requiresParameters(TaskType type) {
		return type != TaskType.REPORT;
	}

	private static JsonNode parseJson(String parameters) {
		requireText(parameters, "Parameters are required");
		try {
			return JsonMapper.readTree(parameters);
		}
		catch (IllegalArgumentException exception) {
			throw new BadRequestException("Parameters must be valid JSON: " + exception.getMessage());
		}
	}

	private static void validateRandom(JsonNode node) {
		if (!node.has("min") || !node.has("max")) {
			throw new BadRequestException("RANDOM requires min and max");
		}
		long min = node.get("min").asLong();
		long max = node.get("max").asLong();
		if (min > max) {
			throw new BadRequestException("RANDOM min must be less than or equal to max");
		}
	}

	private static void validateTextTransform(JsonNode node) {
		requireField(node, "text");
		String operation = requireField(node, "operation").asText();
		if (!operation.matches("(?i)upper|lower|reverse|trim")) {
			throw new BadRequestException("TEXT_TRANSFORM operation must be upper, lower, reverse or trim");
		}
	}

	private static void validateHash(JsonNode node) {
		requireField(node, "text");
		String algorithm = requireField(node, "algorithm").asText();
		if (!algorithm.equalsIgnoreCase("MD5") && !algorithm.equalsIgnoreCase("SHA-256")) {
			throw new BadRequestException("HASH algorithm must be MD5 or SHA-256");
		}
	}

	private static void validateHttpRequest(JsonNode node) {
		requireField(node, "url");
		if (node.has("method")) {
			String method = node.get("method").asText().toUpperCase();
			if (!method.matches("GET|POST|PUT|PATCH|DELETE|HEAD")) {
				throw new BadRequestException("HTTP_REQUEST method is not supported");
			}
		}
	}

	private static void validateFileCheck(JsonNode node) {
		requireField(node, "path");
		String check = node.has("check") ? node.get("check").asText() : "exists";
		if (!check.matches("(?i)exists|size|isDirectory")) {
			throw new BadRequestException("FILE_CHECK check must be exists, size or isDirectory");
		}
	}

	private static void validateSqlQuery(JsonNode node) {
		String sql = requireField(node, "sql").asText().trim();
		SqlGuard.validateSelect(sql);
	}

	private static void validateCleanup(JsonNode node) {
		String target = requireField(node, "target").asText();
		if (!target.equalsIgnoreCase("logs") && !target.equalsIgnoreCase("tasks")) {
			throw new BadRequestException("CLEANUP target must be logs or tasks");
		}
		requirePositiveInt(node, "daysOld", "CLEANUP daysOld is required");
	}

	private static void validateBatch(JsonNode node) {
		if (!node.has("steps") || !node.get("steps").isArray() || node.get("steps").isEmpty()) {
			throw new BadRequestException("BATCH requires non-empty steps array");
		}
		for (JsonNode step : node.get("steps")) {
			if (!step.has("type")) {
				throw new BadRequestException("Each BATCH step requires type");
			}
			TaskType stepType = TaskType.valueOf(step.get("type").asText());
			if (stepType == TaskType.BATCH) {
				throw new BadRequestException("Nested BATCH steps are not allowed");
			}
			if (TaskParameterValidator.requiresParameters(stepType)) {
				String stepParameters = extractStepParameters(step);
				TaskParameterValidator.validate(stepType, stepParameters);
			}
		}
	}

	private static String extractStepParameters(JsonNode step) {
		if (!step.has("parameters")) {
			return null;
		}
		JsonNode parameters = step.get("parameters");
		if (parameters.isTextual()) {
			return parameters.asText();
		}
		return parameters.toString();
	}

	private static void validateSimulation(JsonNode node) {
		requirePositiveInt(node, "durationSeconds", "SIMULATION durationSeconds is required");
		if (node.has("intensity")) {
			int intensity = node.get("intensity").asInt();
			if (intensity < 1 || intensity > 100) {
				throw new BadRequestException("SIMULATION intensity must be between 1 and 100");
			}
		}
	}

	private static void requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new BadRequestException(message);
		}
	}

	private static JsonNode requireField(JsonNode node, String field) {
		if (!node.has(field) || node.get(field).isNull() || node.get(field).asText().isBlank()) {
			throw new BadRequestException("Missing required parameter: " + field);
		}
		return node.get(field);
	}

	private static void requirePositiveInt(JsonNode node, String field, String message) {
		if (!node.has(field)) {
			throw new BadRequestException(message);
		}
		int value = node.get(field).asInt();
		if (value <= 0) {
			throw new BadRequestException(field + " must be greater than zero");
		}
	}
}
