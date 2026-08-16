package ru.asocial.task.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.asocial.task.dto.PageResponse;
import ru.asocial.task.dto.TaskLogResponse;
import ru.asocial.task.dto.TaskLogTableRowResponse;
import ru.asocial.task.model.Task;
import ru.asocial.task.model.TaskLog;
import ru.asocial.task.model.TaskType;
import ru.asocial.task.repository.TaskLogRepository;

@Service
public class TaskLogService {

	private final TaskLogRepository taskLogRepository;
	private final TaskService taskService;

	public TaskLogService(TaskLogRepository taskLogRepository, TaskService taskService) {
		this.taskLogRepository = taskLogRepository;
		this.taskService = taskService;
	}

	@Transactional
	public TaskLogResponse addLog(Long taskId, String message) {
		Task task = taskService.getTaskEntity(taskId);

		TaskLog log = new TaskLog();
		log.setTask(task);
		log.setMessage(message);
		log.setCreatedAt(LocalDateTime.now());

		return toResponse(taskLogRepository.save(log));
	}

	@Transactional(readOnly = true)
	public List<TaskLogResponse> getLogsForTask(Long taskId) {
		taskService.getTaskEntity(taskId);
		return taskLogRepository.findByTask_IdOrderByCreatedAtAsc(taskId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public PageResponse<TaskLogTableRowResponse> getAllLogsForTable(int page, int size) {
		int normalizedPage = PageResponse.normalizePage(page);
		int normalizedSize = PageResponse.normalizeSize(size);
		Pageable pageable = PageRequest.of(
				normalizedPage - 1,
				normalizedSize,
				Sort.by(Sort.Direction.DESC, "createdAt", "id"));

		Page<TaskLogTableRowResponse> result = taskLogRepository.findAllByOrderByCreatedAtDescIdDesc(pageable)
				.map(this::toTableRow);

		return PageResponse.from(result);
	}

	private TaskLogTableRowResponse toTableRow(TaskLog log) {
		Task task = log.getTask();
		return new TaskLogTableRowResponse(
				log.getId(),
				task.getId(),
				formatTaskName(task),
				log.getMessage(),
				log.getCreatedAt());
	}

	private String formatTaskName(Task task) {
		if (task.getType() == TaskType.CALCULATION) {
			if (task.getFormula() != null && !task.getFormula().isBlank()) {
				return "Расчёт: " + task.getFormula();
			}
			return "Расчёт";
		}

		return task.getType().name();
	}

	private TaskLogResponse toResponse(TaskLog log) {
		return new TaskLogResponse(log.getId(), log.getMessage(), log.getCreatedAt());
	}
}
