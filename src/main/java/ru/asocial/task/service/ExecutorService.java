package ru.asocial.task.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.asocial.task.dto.ExecutorCreateRequest;
import ru.asocial.task.dto.ExecutorResponse;
import ru.asocial.task.dto.ExecutorTableRowResponse;
import ru.asocial.task.exception.BadRequestException;
import ru.asocial.task.exception.ResourceNotFoundException;
import ru.asocial.task.model.Executor;
import ru.asocial.task.model.ExecutorState;
import ru.asocial.task.repository.ExecutorRepository;

@Service
public class ExecutorService {

	private final ExecutorRepository executorRepository;

	public ExecutorService(ExecutorRepository executorRepository) {
		this.executorRepository = executorRepository;
	}

	@Transactional(readOnly = true)
	public List<ExecutorTableRowResponse> getExecutorsForTable() {
		return executorRepository.findAll().stream()
				.map(this::toResponse)
				.map(ExecutorTableRowResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<Executor> findAllExecutors() {
		return executorRepository.findAll();
	}

	@Transactional
	public void resetAllToIdle() {
		executorRepository.findAll().forEach(executor -> {
			executor.setState(ExecutorState.IDLE);
			executorRepository.save(executor);
		});
	}

	@Transactional
	public ExecutorResponse createExecutor(ExecutorCreateRequest request) {
		if (request.name() == null || request.name().isBlank()) {
			throw new BadRequestException("Executor name is required");
		}

		if (executorRepository.existsByName(request.name().trim())) {
			throw new BadRequestException("Executor with this name already exists");
		}

		Executor executor = new Executor();
		executor.setName(request.name().trim());
		executor.setState(ExecutorState.IDLE);

		return toResponse(executorRepository.save(executor));
	}

	@Transactional
	public ExecutorResponse updateExecutorState(Long id, ExecutorState state) {
		if (state == null) {
			throw new BadRequestException("Executor state is required");
		}

		Executor executor = getExecutorEntity(id);
		executor.setState(state);
		return toResponse(executorRepository.save(executor));
	}

	@Transactional(readOnly = true)
	public Executor getExecutorEntity(Long id) {
		return executorRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Executor not found: " + id));
	}

	@Transactional
	public void setExecutorState(Long id, ExecutorState state) {
		Executor executor = getExecutorEntity(id);
		executor.setState(state);
		executorRepository.save(executor);
	}

	private ExecutorResponse toResponse(Executor executor) {
		return new ExecutorResponse(
				executor.getId(),
				executor.getName(),
				executor.getState(),
				executor.getSuccessfulTasksCount(),
				executor.getErrorTasksCount(),
				executor.getAbortedTasksCount());
	}
}
