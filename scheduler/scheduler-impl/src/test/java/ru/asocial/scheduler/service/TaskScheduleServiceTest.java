package ru.asocial.scheduler.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import ru.asocial.scheduler.model.TaskSchedule;
import ru.asocial.scheduler.repository.TaskScheduleRepository;
import ru.asocial.task.dto.TaskCreateCommand;
import ru.asocial.task.model.TaskType;

@ExtendWith(MockitoExtension.class)
class TaskScheduleServiceTest {

	@Mock
	private TaskScheduleRepository taskScheduleRepository;

	@Mock
	private KafkaTemplate<String, TaskCreateCommand> kafkaTemplate;

	@InjectMocks
	private TaskScheduleService taskScheduleService;

	@Test
	void triggerSchedulePublishesTaskCreateCommand() {
		ReflectionTestUtils.setField(taskScheduleService, "taskCreateTopic", "task.create");

		TaskSchedule schedule = new TaskSchedule();
		schedule.setId(7L);
		schedule.setTaskType(TaskType.DELAY);
		schedule.setFormula("{\"durationSeconds\":30}");
		schedule.setEnabled(true);
		schedule.setRepeatIntervalMinutes(null);

		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		when(taskScheduleRepository.save(schedule)).thenReturn(schedule);

		ReflectionTestUtils.invokeMethod(taskScheduleService, "triggerSchedule", schedule, now);

		ArgumentCaptor<TaskCreateCommand> commandCaptor = ArgumentCaptor.forClass(TaskCreateCommand.class);
		verify(kafkaTemplate).send(eq("task.create"), eq("7"), commandCaptor.capture());
		verify(taskScheduleRepository).save(schedule);
	}
}
