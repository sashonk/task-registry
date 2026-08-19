package ru.asocial.task.controller;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
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

import ru.asocial.task.dto.ExecutorCreateRequest;
import ru.asocial.task.dto.ExecutorResponse;
import ru.asocial.task.dto.ExecutorStateUpdateRequest;
import ru.asocial.task.dto.ExecutorTableRowResponse;
import ru.asocial.task.exception.BadRequestException;
import ru.asocial.task.service.ExecutorService;
import ru.asocial.task.service.WorkerService;

@RestController
@RequestMapping("/api/executors")
public class ExecutorController {

	private final ExecutorService executorService;
	private final ObjectProvider<WorkerService> workerService;

	public ExecutorController(ExecutorService executorService, ObjectProvider<WorkerService> workerService) {
		this.executorService = executorService;
		this.workerService = workerService;
	}

	@GetMapping
	public List<ExecutorTableRowResponse> getExecutorsForTable() {
		return executorService.getExecutorsForTable();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ExecutorResponse createExecutor(@RequestBody ExecutorCreateRequest request) {
		return executorService.createExecutor(request);
	}

	@PostMapping("/worker")
	@ResponseStatus(HttpStatus.CREATED)
	public ExecutorResponse spawnWorker() {
		WorkerService worker = workerService.getIfAvailable();
		if (worker == null) {
			throw new BadRequestException("Workers are disabled");
		}
		return worker.spawnWorker();
	}

	@PatchMapping("/{id}/state")
	public ExecutorResponse updateExecutorState(@PathVariable Long id, @RequestBody ExecutorStateUpdateRequest request) {
		return executorService.updateExecutorState(id, request.state());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteExecutor(@PathVariable Long id) {
		WorkerService worker = workerService.getIfAvailable();
		if (worker != null) {
			worker.stopAndDelete(id);
			return;
		}
		executorService.deleteExecutor(id);
	}
}
