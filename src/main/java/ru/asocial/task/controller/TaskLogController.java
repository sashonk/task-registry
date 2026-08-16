package ru.asocial.task.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
	public List<TaskLogTableRowResponse> getAllLogsForTable() {
		return taskLogService.getAllLogsForTable();
	}
}
