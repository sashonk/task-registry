package ru.asocial.task.kafka;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.asocial.task.dto.TaskCreateRequest;
import ru.asocial.task.dto.TaskResponse;
import ru.asocial.task.exception.BadRequestException;
import ru.asocial.task.model.TaskStatus;
import ru.asocial.task.model.TaskType;
import ru.asocial.task.service.TaskService;

@ExtendWith(MockitoExtension.class)
class TaskCreateKafkaListenerTest {

	@Mock
	private TaskService taskService;

	@InjectMocks
	private TaskCreateKafkaListener listener;

	@Test
	void createsTaskFromMessage() {
		TaskCreateRequest request = new TaskCreateRequest(TaskType.DELAY, "{\"durationSeconds\":30}");
		TaskResponse response = new TaskResponse(
				1L,
				TaskType.DELAY,
				null,
				LocalDateTime.now(),
				null,
				null,
				TaskStatus.NEW,
				0L,
				request.parameters());

		when(taskService.createTask(request)).thenReturn(response);

		listener.onTaskCreate(request);

		verify(taskService).createTask(request);
	}

	@Test
	void propagatesBadRequestException() {
		TaskCreateRequest request = new TaskCreateRequest(null, null);

		when(taskService.createTask(request)).thenThrow(new BadRequestException("Task type is required"));

		assertThrows(BadRequestException.class, () -> listener.onTaskCreate(request));
	}
}
