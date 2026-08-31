package ru.asocial.scheduler.validation;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class JsonMapper {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private JsonMapper() {
	}

	static JsonNode readTree(String json) {
		try {
			return OBJECT_MAPPER.readTree(json);
		}
		catch (JacksonException exception) {
			throw new IllegalArgumentException(exception.getOriginalMessage());
		}
	}
}
