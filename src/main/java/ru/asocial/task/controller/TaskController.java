package ru.asocial.task.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.asocial.task.dto.PageResponse;
import ru.asocial.task.dto.TaskCreateRequest;
import ru.asocial.task.dto.TaskLogResponse;
import ru.asocial.task.dto.TaskResponse;
import ru.asocial.task.dto.TaskStatusUpdateRequest;
import ru.asocial.task.dto.TaskTableRowResponse;
import ru.asocial.task.service.TaskLogService;
import ru.asocial.task.service.TaskService;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

	private final TaskService taskService;
	private final TaskLogService taskLogService;

	public TaskController(TaskService taskService, TaskLogService taskLogService) {
		this.taskService = taskService;
		this.taskLogService = taskLogService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TaskResponse createTask(@RequestBody TaskCreateRequest request) {
		return taskService.createTask(request);
	}

	@GetMapping("/{id}")
	public TaskResponse getTask(@PathVariable Long id) {
		return taskService.getTaskById(id);
	}

	@PatchMapping("/{id}/status")
	public TaskResponse updateTaskStatus(@PathVariable Long id, @RequestBody TaskStatusUpdateRequest request) {
		return taskService.updateTaskStatus(id, request.status());
	}

	@GetMapping
	public PageResponse<TaskTableRowResponse> getTasksForTable(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size) {
		return taskService.getTasksForTable(page, size);
	}

	@GetMapping("/{id}/logs")
	public List<TaskLogResponse> getTaskLogs(@PathVariable Long id) {
		return taskLogService.getLogsForTask(id);
	}
}
