package ru.asocial.scheduler.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ru.asocial.scheduler.dto.ScheduleCreateRequest;
import ru.asocial.scheduler.dto.ScheduleEnabledUpdateRequest;
import ru.asocial.scheduler.dto.ScheduleResponse;
import ru.asocial.scheduler.dto.ScheduleTableRowResponse;
import ru.asocial.scheduler.service.TaskScheduleService;

@RestController
@RequestMapping("/api/schedules")
public class TaskScheduleController {

	private final TaskScheduleService taskScheduleService;

	public TaskScheduleController(TaskScheduleService taskScheduleService) {
		this.taskScheduleService = taskScheduleService;
	}

	@GetMapping
	public List<ScheduleTableRowResponse> getSchedulesForTable() {
		return taskScheduleService.getSchedulesForTable();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ScheduleResponse createSchedule(@RequestBody ScheduleCreateRequest request) {
		return taskScheduleService.createSchedule(request);
	}

	@PatchMapping("/{id}/enabled")
	public ScheduleResponse updateEnabled(@PathVariable Long id, @RequestBody ScheduleEnabledUpdateRequest request) {
		return taskScheduleService.updateEnabled(id, request.enabled());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteSchedule(@PathVariable Long id) {
		taskScheduleService.deleteSchedule(id);
	}
}
