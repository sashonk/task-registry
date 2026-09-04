package ru.asocial.task.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@DependsOn("entityManagerFactory")
public class H2CheckConstraintMigration implements InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(H2CheckConstraintMigration.class);

	private static final List<String> TABLES = List.of("tasks", "executors");

	private final JdbcTemplate jdbcTemplate;

	public H2CheckConstraintMigration(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void afterPropertiesSet() {
		if (!isH2Database()) {
			return;
		}

		for (String table : TABLES) {
			dropChecks(table);
		}
	}

	private void dropChecks(String table) {
		List<String> constraintNames = jdbcTemplate.query(
				"""
						SELECT CONSTRAINT_NAME
						FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
						WHERE CONSTRAINT_TYPE = 'CHECK'
						  AND UPPER(TABLE_NAME) = ?
						""",
				(rs, rowNum) -> rs.getString(1),
				table.toUpperCase());

		for (String constraintName : constraintNames) {
			if (constraintName == null || constraintName.isBlank()) {
				continue;
			}
			jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS \""
					+ constraintName.replace("\"", "") + "\"");
			log.info("Dropped H2 check constraint {} on {}", constraintName, table);
		}
	}

	private boolean isH2Database() {
		try (var connection = jdbcTemplate.getDataSource().getConnection()) {
			return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("h2");
		}
		catch (Exception exception) {
			log.warn("Could not detect database product for check constraint migration", exception);
			return false;
		}
	}
}
