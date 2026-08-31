package ru.asocial.task.service.task;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class TaskParametersMapper {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private TaskParametersMapper() {
	}

	public static JsonNode readTree(String json) {
		try {
			return OBJECT_MAPPER.readTree(json);
		}
		catch (JacksonException exception) {
			throw new IllegalArgumentException(exception.getOriginalMessage());
		}
	}

	public static String writeValue(Object value) {
		try {
			return OBJECT_MAPPER.writeValueAsString(value);
		}
		catch (JacksonException exception) {
			throw new IllegalArgumentException(exception.getOriginalMessage());
		}
	}
}
