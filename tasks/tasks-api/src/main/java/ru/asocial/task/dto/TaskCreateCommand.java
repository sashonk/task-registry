package ru.asocial.task.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import ru.asocial.task.model.TaskType;

public record TaskCreateCommand(
		TaskType type,
		@JsonAlias("formula") String parameters) {
}
