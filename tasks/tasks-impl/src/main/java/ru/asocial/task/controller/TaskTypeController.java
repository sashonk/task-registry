package ru.asocial.task.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.asocial.task.dto.TaskTypeInfoResponse;
import ru.asocial.task.service.task.TaskTypeCatalog;

@RestController
@RequestMapping("/api/task-types")
public class TaskTypeController {

	private final TaskTypeCatalog taskTypeCatalog;

	public TaskTypeController(TaskTypeCatalog taskTypeCatalog) {
		this.taskTypeCatalog = taskTypeCatalog;
	}

	@GetMapping
	public List<TaskTypeInfoResponse> getTaskTypes() {
		return taskTypeCatalog.getAll();
	}
}
