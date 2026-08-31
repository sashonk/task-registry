package ru.asocial.task.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class NoopTaskMigration {

	private static final Logger log = LoggerFactory.getLogger(NoopTaskMigration.class);

	private final JdbcTemplate jdbcTemplate;

	public NoopTaskMigration(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void removeNoopTasks() {
		int logCount = jdbcTemplate.update(
				"DELETE FROM task_logs WHERE task_id IN (SELECT id FROM tasks WHERE type = 'NOOP')");
		int taskCount = jdbcTemplate.update("DELETE FROM tasks WHERE type = 'NOOP'");

		if (logCount + taskCount > 0) {
			log.info("Removed NOOP data: {} logs, {} tasks", logCount, taskCount);
		}
	}
}
