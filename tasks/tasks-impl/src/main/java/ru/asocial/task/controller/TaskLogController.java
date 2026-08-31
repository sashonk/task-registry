package ru.asocial.task.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.asocial.task.dto.PageResponse;
import ru.asocial.task.dto.TaskLogTableRowResponse;
import ru.asocial.task.service.TaskLogService;

@RestController
@RequestMapping("/api/logs")
public class TaskLogController {

	private final TaskLogService taskLogService;

	public TaskLogController(TaskLogService taskLogService) {
		this.taskLogService = taskLogService;
	}

	@GetMapping
	public PageResponse<TaskLogTableRowResponse> getAllLogsForTable(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size) {
		return taskLogService.getAllLogsForTable(page, size);
	}
}
