package ru.asocial.task.service.task;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.asocial.task.model.TaskStatus;
import ru.asocial.task.repository.TaskLogRepository;
import ru.asocial.task.repository.TaskRepository;

@Service
public class TaskCleanupService {

	private static final List<TaskStatus> TERMINAL_STATUSES = List.of(
			TaskStatus.DONE, TaskStatus.ERROR, TaskStatus.ABORTED);

	private final TaskLogRepository taskLogRepository;
	private final TaskRepository taskRepository;

	public TaskCleanupService(TaskLogRepository taskLogRepository, TaskRepository taskRepository) {
		this.taskLogRepository = taskLogRepository;
		this.taskRepository = taskRepository;
	}

	@Transactional
	public int cleanup(String target, int daysOld) {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);

		if (target.equals("logs")) {
			return taskLogRepository.deleteByCreatedAtBefore(cutoff);
		}

		int deleted = taskLogRepository.deleteLogsForCompletedTasksBefore(cutoff, TERMINAL_STATUSES);
		deleted += taskRepository.deleteTerminalTasksBefore(cutoff, TERMINAL_STATUSES);
		return deleted;
	}
}
