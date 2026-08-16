package ru.asocial.task.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TaskPayloadColumnMigration {

	private static final Logger log = LoggerFactory.getLogger(TaskPayloadColumnMigration.class);

	private final JdbcTemplate jdbcTemplate;

	public TaskPayloadColumnMigration(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void migratePayloadColumns() {
		if (!isH2Database()) {
			return;
		}

		extendColumn("tasks", "formula");
		extendColumn("task_schedules", "formula");
		extendTypeColumn("tasks", "type");
		extendTypeColumn("task_schedules", "task_type");
	}

	private boolean isH2Database() {
		try (var connection = jdbcTemplate.getDataSource().getConnection()) {
			return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("h2");
		}
		catch (Exception exception) {
			log.warn("Could not detect database product for payload migration", exception);
			return false;
		}
	}

	private void extendColumn(String table, String column) {
		try {
			jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN " + column + " VARCHAR(4000)");
			log.info("Migrated {}.{} column to VARCHAR(4000)", table, column);
		}
		catch (Exception exception) {
			log.debug("{}.{} column migration skipped: {}", table, column, exception.getMessage());
		}
	}

	private void extendTypeColumn(String table, String column) {
		try {
			jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN " + column + " VARCHAR(32)");
			log.info("Migrated {}.{} column to VARCHAR(32)", table, column);
		}
		catch (Exception exception) {
			log.debug("{}.{} column migration skipped: {}", table, column, exception.getMessage());
		}
	}
}
