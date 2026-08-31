package ru.asocial.task.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StoppedExecutorStateMigration {

	private static final Logger log = LoggerFactory.getLogger(StoppedExecutorStateMigration.class);

	private final JdbcTemplate jdbcTemplate;

	public StoppedExecutorStateMigration(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	@EventListener(ApplicationReadyEvent.class)
	public void migrateStoppedExecutorsToIdle() {
		try {
			int updated = jdbcTemplate.update("UPDATE executors SET state = 'IDLE' WHERE state = 'STOPPED'");
			if (updated > 0) {
				log.info("Migrated {} stopped executor(s) to IDLE", updated);
			}
		}
		catch (Exception exception) {
			log.debug("STOPPED executor state migration skipped: {}", exception.getMessage());
		}
	}
}
