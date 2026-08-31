package ru.asocial.task.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import ru.asocial.task.dto.TaskCreateCommand;
import ru.asocial.task.dto.TaskResponse;
import ru.asocial.task.service.TaskService;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class TaskCreateKafkaListener {

	private static final Logger log = LoggerFactory.getLogger(TaskCreateKafkaListener.class);

	private final TaskService taskService;

	public TaskCreateKafkaListener(TaskService taskService) {
		this.taskService = taskService;
	}

	@KafkaListener(
			topics = "${app.kafka.task-create-topic}",
			groupId = "${spring.kafka.consumer.group-id}")
	public void onTaskCreate(TaskCreateCommand request) {
		TaskResponse response = taskService.createTask(request);
		log.info("Task created from Kafka: id={}, type={}", response.id(), response.type());
	}
}
