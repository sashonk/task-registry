package ru.asocial.scheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduleRunnerService {

	private static final Logger log = LoggerFactory.getLogger(ScheduleRunnerService.class);

	private final TaskScheduleService taskScheduleService;

	public ScheduleRunnerService(TaskScheduleService taskScheduleService) {
		this.taskScheduleService = taskScheduleService;
	}

	@Scheduled(fixedDelayString = "${scheduler.poll-interval-ms:5000}")
	public void runDueSchedules() {
		try {
			taskScheduleService.processDueSchedules();
		}
		catch (Exception exception) {
			log.error("Failed to process due schedules", exception);
		}
	}
}
