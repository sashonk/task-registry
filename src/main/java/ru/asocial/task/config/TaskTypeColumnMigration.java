package ru.asocial.task.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TaskTypeColumnMigration {

	private static final Logger log = LoggerFactory.getLogger(TaskTypeColumnMigration.class);

	private final JdbcTemplate jdbcTemplate;

	public TaskTypeColumnMigration(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void migrateTaskTypeColumn() {
		try (var connection = jdbcTemplate.getDataSource().getConnection()) {
			String databaseProduct = connection.getMetaData().getDatabaseProductName();
			if (!databaseProduct.toLowerCase().contains("h2")) {
				return;
			}
		}
		catch (Exception exception) {
			log.warn("Could not detect database product for task type migration", exception);
			return;
		}

		try {
			jdbcTemplate.execute("ALTER TABLE tasks ALTER COLUMN type VARCHAR(32) NOT NULL");
			log.info("Migrated tasks.type column to VARCHAR(32)");
		}
		catch (Exception exception) {
			log.debug("tasks.type column migration skipped: {}", exception.getMessage());
		}
	}
}
