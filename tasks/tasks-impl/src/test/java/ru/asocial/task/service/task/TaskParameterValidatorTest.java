package ru.asocial.task.service.task;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import ru.asocial.task.exception.BadRequestException;
import ru.asocial.task.model.TaskType;

class TaskParameterValidatorTest {

	@Test
	void acceptsValidRssReadParameters() {
		assertDoesNotThrow(() -> TaskParameterValidator.validate(
				TaskType.RSS_READ,
				"{\"url\": \"https://example.com/feed.xml\", \"maxItems\": 10}"));
	}

	@Test
	void rejectsRssReadWithoutUrl() {
		BadRequestException exception = assertThrows(
				BadRequestException.class,
				() -> TaskParameterValidator.validate(TaskType.RSS_READ, "{\"maxItems\": 5}"));

		assertEquals("Missing required parameter: url", exception.getMessage());
	}

	@Test
	void rejectsRssReadWhenMaxItemsIsNotPositive() {
		BadRequestException exception = assertThrows(
				BadRequestException.class,
				() -> TaskParameterValidator.validate(
						TaskType.RSS_READ,
						"{\"url\": \"https://example.com/feed.xml\", \"maxItems\": 0}"));

		assertEquals("RSS_READ maxItems must be between 1 and 50", exception.getMessage());
	}

	@Test
	void rejectsRssReadWhenMaxItemsExceedsLimit() {
		BadRequestException exception = assertThrows(
				BadRequestException.class,
				() -> TaskParameterValidator.validate(
						TaskType.RSS_READ,
						"{\"url\": \"https://example.com/feed.xml\", \"maxItems\": 51}"));

		assertEquals("RSS_READ maxItems must be between 1 and 50", exception.getMessage());
	}
}
