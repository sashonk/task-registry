package ru.asocial.task.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.asocial.task.dto.PageResponse;
import ru.asocial.task.dto.TaskCreateCommand;
import ru.asocial.task.dto.TaskResponse;
import ru.asocial.task.dto.TaskTableRowResponse;
import ru.asocial.task.exception.BadRequestException;
import ru.asocial.task.exception.ResourceNotFoundException;
import ru.asocial.task.model.Executor;
import ru.asocial.task.model.ExecutorState;
import ru.asocial.task.model.Task;
import ru.asocial.task.model.TaskStatus;
import ru.asocial.task.service.task.TaskDisplayNames;
import ru.asocial.task.service.task.TaskParameterValidator;
import ru.asocial.task.repository.ExecutorRepository;
import ru.asocial.task.repository.TaskRepository;

@Service
public class TaskService {

	private static final Logger log = LoggerFactory.getLogger(TaskService.class);

	private final TaskRepository taskRepository;
	private final ExecutorRepository executorRepository;
	private final AuditEventProducer auditProducer;

	public TaskService(TaskRepository taskRepository,
			ExecutorRepository executorRepository,
			AuditEventProducer auditProducer) {
		this.taskRepository = taskRepository;
		this.executorRepository = executorRepository;
		this.auditProducer = auditProducer;
	}

	@Transactional
	public TaskResponse createTask(TaskCreateCommand request) {
		if (request.type() == null) {
			throw new BadRequestException("Task type is required");
		}

		if (TaskParameterValidator.requiresParameters(request.type())) {
			TaskParameterValidator.validate(request.type(), request.parameters());
		}

		Task task = new Task();
		task.setType(request.type());
		task.setStatus(TaskStatus.NEW);
		task.setCompletionPercent(0L);
		task.setCreatedAt(LocalDateTime.now());
		task.setFormula(normalizeParameters(request));

		Task savedTask = taskRepository.save(task);

		try {
			auditProducer.sendTaskCreatedEvent(savedTask.getId(), savedTask.getType().name(), savedTask.getFormula());
		} catch (Exception e) {
			log.warn("Failed to send audit event for task creation: {}", savedTask.getId(), e);
		}

		return toResponse(savedTask);
	}

	private String normalizeParameters(TaskCreateCommand request) {
		if (request.parameters() == null || request.parameters().isBlank()) {
			return null;
		}
		return request.parameters().trim();
	}

	@Transactional(readOnly = true)
	public TaskResponse getTaskById(Long id) {
		return toResponse(getTaskEntity(id));
	}

	@Transactional(readOnly = true)
	public PageResponse<TaskTableRowResponse> getTasksForTable(int page, int size) {
		int normalizedPage = PageResponse.normalizePage(page);
		int normalizedSize = PageResponse.normalizeSize(size);
		Pageable pageable = PageRequest.of(
				normalizedPage - 1,
				normalizedSize,
				Sort.by(Sort.Direction.DESC, "createdAt", "id"));

		Page<TaskTableRowResponse> result = taskRepository.findAllByOrderByCreatedAtDescIdDesc(pageable)
				.map(this::toResponse)
				.map(TaskTableRowResponse::from);

		return PageResponse.from(result);
	}

	@Transactional
	public TaskResponse updateTaskStatus(Long id, TaskStatus status) {
		if (status == null) {
			throw new BadRequestException("Task status is required");
		}

		Task task = getTaskEntity(id);
		TaskStatus previousStatus = task.getStatus();
		task.setStatus(status);

		if (status == TaskStatus.DONE || status == TaskStatus.ERROR || status == TaskStatus.ABORTED) {
			if (task.getCompletedAt() == null) {
				task.setCompletedAt(LocalDateTime.now());
			}
			if (status == TaskStatus.DONE) {
				task.setCompletionPercent(100L);
			}
			recordExecutorTaskOutcome(task, previousStatus, status);
			releaseExecutor(task);
		}

		if (status == TaskStatus.NEW) {
			task.setStartedAt(null);
			task.setCompletedAt(null);
			task.setCompletionPercent(0L);
			task.setExecutor(null);
		}

		Task savedTask = taskRepository.save(task);

		try {
			auditProducer.sendTaskStatusChangedEvent(id, previousStatus.name(), status.name());
		} catch (Exception e) {
			log.warn("Failed to send audit event for task status change: {}", id, e);
		}

		return toResponse(savedTask);
	}

	@Transactional
	public void updateProgress(Long taskId, long completionPercent) {
		Task task = getTaskEntity(taskId);
		task.setCompletionPercent(Math.min(100L, Math.max(0L, completionPercent)));
		taskRepository.save(task);
	}

	@Transactional(readOnly = true)
	public Task getTaskEntity(Long id) {
		return taskRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
	}

	@Transactional
	public Optional<Task> claimNextTask(Long executorId) {
		Executor executor = executorRepository.findByIdForUpdate(executorId)
				.orElseThrow(() -> new ResourceNotFoundException("Executor not found: " + executorId));

		if (executor.getState() != ExecutorState.IDLE) {
			return Optional.empty();
		}

		Optional<Task> taskOptional = taskRepository.findFirstByStatusOrderByCreatedAtAsc(TaskStatus.NEW);
		if (taskOptional.isEmpty()) {
			return Optional.empty();
		}

		Task task = taskOptional.get();
		task.setExecutor(executor);
		task.setStatus(TaskStatus.IN_PROGRESS);
		task.setStartedAt(LocalDateTime.now());
		executor.setState(ExecutorState.WORKING);

		taskRepository.save(task);
		executorRepository.save(executor);
		return Optional.of(task);
	}

	@Transactional
	public void completeTask(Long taskId) {
		Task task = getTaskEntity(taskId);
		TaskStatus previousStatus = task.getStatus();
		task.setStatus(TaskStatus.DONE);
		task.setCompletionPercent(100L);
		task.setCompletedAt(LocalDateTime.now());
		recordExecutorTaskOutcome(task, previousStatus, TaskStatus.DONE);
		taskRepository.save(task);
		releaseExecutor(task);
	}

	private TaskResponse toResponse(Task task) {
		String executorName = task.getExecutor() != null ? task.getExecutor().getName() : null;
		return new TaskResponse(
				task.getId(),
				task.getType(),
				executorName,
				task.getCreatedAt(),
				task.getStartedAt(),
				task.getCompletedAt(),
				task.getStatus(),
				task.getCompletionPercent(),
				task.getFormula());
	}

	private void releaseExecutor(Task task) {
		if (task.getExecutor() == null) {
			return;
		}

		Executor executor = executorRepository.findByIdForUpdate(task.getExecutor().getId()).orElse(null);
		if (executor == null) {
			return;
		}

		executor.setState(ExecutorState.IDLE);
		executorRepository.save(executor);
	}

	private void recordExecutorTaskOutcome(Task task, TaskStatus previousStatus, TaskStatus newStatus) {
		if (task.getExecutor() == null || previousStatus == newStatus) {
			return;
		}
		if (!isTerminalStatus(newStatus) || isTerminalStatus(previousStatus)) {
			return;
		}

		Executor executor = executorRepository.findByIdForUpdate(task.getExecutor().getId()).orElse(null);
		if (executor == null) {
			return;
		}

		switch (newStatus) {
			case DONE -> executor.setSuccessfulTasksCount(executor.getSuccessfulTasksCount() + 1);
			case ERROR -> executor.setErrorTasksCount(executor.getErrorTasksCount() + 1);
			case ABORTED -> executor.setAbortedTasksCount(executor.getAbortedTasksCount() + 1);
			default -> {
			}
		}
		executorRepository.save(executor);
	}

	private boolean isTerminalStatus(TaskStatus status) {
		return status == TaskStatus.DONE || status == TaskStatus.ERROR || status == TaskStatus.ABORTED;
	}
}
