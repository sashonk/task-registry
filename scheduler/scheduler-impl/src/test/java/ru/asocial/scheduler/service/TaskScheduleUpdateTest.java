package ru.asocial.scheduler.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.asocial.scheduler.dto.ScheduleCreateRequest;
import ru.asocial.scheduler.dto.ScheduleResponse;
import ru.asocial.task.model.TaskType;

@SpringBootTest(properties = {
		"scheduler.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:scheduleupdate;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"app.jwt.secret=test-secret-key-at-least-32-characters-long",
		"app.jwt.issuer=jobflow-auth",
		"app.jwt.access-token-ttl=PT1H"
})
class TaskScheduleUpdateTest {

	@Autowired
	private TaskScheduleService taskScheduleService;

	@Test
	void updateEnabledRewritesScheduleWithoutH2CheckConstraintError() {
		ScheduleResponse created = taskScheduleService.createSchedule(new ScheduleCreateRequest(
				TaskType.REPORT,
				null,
				Instant.parse("2099-01-01T00:00:00Z"),
				60L));

		ScheduleResponse updated = taskScheduleService.updateEnabled(created.id(), false);

		assertThat(updated.enabled()).isFalse();
		assertThat(updated.taskType()).isEqualTo(TaskType.REPORT);
	}
}
