package ru.asocial.task.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ExecutorStatsColumnMigration {

	private static final Logger log = LoggerFactory.getLogger(ExecutorStatsColumnMigration.class);

	private final JdbcTemplate jdbcTemplate;

	public ExecutorStatsColumnMigration(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void migrateExecutorStatsColumns() {
		if (!isH2Database()) {
			return;
		}

		addColumnIfMissing("successful_tasks_count", "BIGINT NOT NULL DEFAULT 0");
		addColumnIfMissing("error_tasks_count", "BIGINT NOT NULL DEFAULT 0");
		addColumnIfMissing("aborted_tasks_count", "BIGINT NOT NULL DEFAULT 0");
		backfillExecutorStats();
	}

	private boolean isH2Database() {
		try (var connection = jdbcTemplate.getDataSource().getConnection()) {
			return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("h2");
		}
		catch (Exception exception) {
			log.warn("Could not detect database product for executor stats migration", exception);
			return false;
		}
	}

	private void addColumnIfMissing(String columnName, String definition) {
		try {
			jdbcTemplate.execute("ALTER TABLE executors ADD COLUMN IF NOT EXISTS " + columnName + " " + definition);
			log.info("Ensured executors.{} column exists", columnName);
		}
		catch (Exception exception) {
			log.debug("executors.{} column migration skipped: {}", columnName, exception.getMessage());
		}
	}

	private void backfillExecutorStats() {
		try {
			jdbcTemplate.update("""
					UPDATE executors e
					SET successful_tasks_count = (
						SELECT COUNT(*) FROM tasks t
						WHERE t.executor_id = e.id AND t.status = 'DONE'
					),
					error_tasks_count = (
						SELECT COUNT(*) FROM tasks t
						WHERE t.executor_id = e.id AND t.status = 'ERROR'
					),
					aborted_tasks_count = (
						SELECT COUNT(*) FROM tasks t
						WHERE t.executor_id = e.id AND t.status = 'ABORTED'
					)
					""");
			log.info("Backfilled executor task statistics from tasks table");
		}
		catch (Exception exception) {
			log.warn("Executor stats backfill skipped: {}", exception.getMessage());
		}
	}
}
