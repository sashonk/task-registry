package ru.asocial.scheduler.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.asocial.scheduler.dto.ScheduleCreateRequest;
import ru.asocial.scheduler.dto.ScheduleResponse;
import ru.asocial.scheduler.dto.ScheduleTableRowResponse;
import ru.asocial.scheduler.exception.BadRequestException;
import ru.asocial.scheduler.exception.ResourceNotFoundException;
import ru.asocial.scheduler.model.TaskSchedule;
import ru.asocial.scheduler.repository.TaskScheduleRepository;
import ru.asocial.scheduler.validation.TaskParameterValidator;
import ru.asocial.task.dto.TaskCreateCommand;

@Service
public class TaskScheduleService {

	private static final Logger log = LoggerFactory.getLogger(TaskScheduleService.class);

	private final TaskScheduleRepository taskScheduleRepository;
	private final KafkaTemplate<String, TaskCreateCommand> kafkaTemplate;
	private final String taskCreateTopic;

	public TaskScheduleService(
			TaskScheduleRepository taskScheduleRepository,
			KafkaTemplate<String, TaskCreateCommand> kafkaTemplate,
			@Value("${app.kafka.task-create-topic}") String taskCreateTopic) {
		this.taskScheduleRepository = taskScheduleRepository;
		this.kafkaTemplate = kafkaTemplate;
		this.taskCreateTopic = taskCreateTopic;
	}

	@Transactional(readOnly = true)
	public List<ScheduleTableRowResponse> getSchedulesForTable() {
		return taskScheduleRepository.findAll().stream()
				.sorted((left, right) -> left.getNextRunAt().compareTo(right.getNextRunAt()))
				.map(this::toResponse)
				.map(ScheduleTableRowResponse::from)
				.toList();
	}

	@Transactional
	public ScheduleResponse createSchedule(ScheduleCreateRequest request) {
		validateCreateRequest(request);

		TaskSchedule schedule = new TaskSchedule();
		schedule.setTaskType(request.taskType());
		schedule.setFormula(normalizeParameters(request));
		schedule.setNextRunAt(request.nextRunAt());
		schedule.setRepeatIntervalMinutes(normalizeRepeatInterval(request.repeatIntervalMinutes()));
		schedule.setEnabled(true);
		schedule.setCreatedAt(Instant.now());

		return toResponse(taskScheduleRepository.save(schedule));
	}

	@Transactional
	public ScheduleResponse updateEnabled(Long id, boolean enabled) {
		TaskSchedule schedule = getScheduleEntity(id);
		schedule.setEnabled(enabled);
		return toResponse(taskScheduleRepository.save(schedule));
	}

	@Transactional
	public void deleteSchedule(Long id) {
		if (!taskScheduleRepository.existsById(id)) {
			throw new ResourceNotFoundException("Schedule not found: " + id);
		}
		taskScheduleRepository.deleteById(id);
	}

	@Transactional
	public void processDueSchedules() {
		Instant now = Instant.now();
		List<TaskSchedule> dueSchedules = taskScheduleRepository
				.findByEnabledTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(now);

		for (TaskSchedule schedule : dueSchedules) {
			triggerSchedule(schedule, now);
		}
	}

	@Transactional(readOnly = true)
	public TaskSchedule getScheduleEntity(Long id) {
		return taskScheduleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + id));
	}

	private void triggerSchedule(TaskSchedule schedule, Instant now) {
		TaskCreateCommand command = new TaskCreateCommand(schedule.getTaskType(), schedule.getFormula());
		kafkaTemplate.send(taskCreateTopic, String.valueOf(schedule.getId()), command);
		log.info("Published task create command for schedule id={}, type={}", schedule.getId(), schedule.getTaskType());

		schedule.setLastTriggeredAt(now);

		Long repeatIntervalMinutes = schedule.getRepeatIntervalMinutes();
		if (repeatIntervalMinutes != null && repeatIntervalMinutes > 0) {
			schedule.setNextRunAt(now.plusSeconds(repeatIntervalMinutes * 60));
		}
		else {
			schedule.setEnabled(false);
		}

		taskScheduleRepository.save(schedule);
	}

	private void validateCreateRequest(ScheduleCreateRequest request) {
		if (request.taskType() == null) {
			throw new BadRequestException("Task type is required");
		}
		if (request.nextRunAt() == null) {
			throw new BadRequestException("Next run time is required");
		}

		if (TaskParameterValidator.requiresParameters(request.taskType())) {
			TaskParameterValidator.validate(request.taskType(), request.parameters());
		}

		if (request.repeatIntervalMinutes() != null && request.repeatIntervalMinutes() <= 0) {
			throw new BadRequestException("Repeat interval must be greater than zero");
		}
	}

	private String normalizeParameters(ScheduleCreateRequest request) {
		if (request.parameters() == null || request.parameters().isBlank()) {
			return null;
		}
		return request.parameters().trim();
	}

	private Long normalizeRepeatInterval(Long repeatIntervalMinutes) {
		if (repeatIntervalMinutes == null || repeatIntervalMinutes <= 0) {
			return null;
		}

		return repeatIntervalMinutes;
	}

	private ScheduleResponse toResponse(TaskSchedule schedule) {
		return new ScheduleResponse(
				schedule.getId(),
				schedule.getTaskType(),
				schedule.getFormula(),
				schedule.getNextRunAt(),
				schedule.getRepeatIntervalMinutes(),
				schedule.isEnabled(),
				schedule.getLastTriggeredAt(),
				schedule.getCreatedAt());
	}
}
