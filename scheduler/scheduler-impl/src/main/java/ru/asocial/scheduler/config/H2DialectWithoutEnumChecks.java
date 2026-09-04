package ru.asocial.scheduler.config;

import org.hibernate.dialect.H2Dialect;

/**
 * H2 2.4.240 evaluates CHECK constraints on the connection that created them.
 * Hibernate adds {@code task_type IN (...)} checks for enums, and later
 * updates from the pool fail with "Check constraint invalid: CONSTRAINT_D".
 */
public class H2DialectWithoutEnumChecks extends H2Dialect {

	@Override
	public String getCheckCondition(String columnName, String[] values) {
		return null;
	}

	@Override
	public String getCheckCondition(String columnName, long min, long max) {
		return null;
	}
}
