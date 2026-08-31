package ru.asocial.task.dto;

import ru.asocial.task.model.TaskStatus;

public record TaskStatusUpdateRequest(TaskStatus status) {
}
