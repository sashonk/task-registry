package ru.asocial.task.config;

import org.hibernate.dialect.H2Dialect;

/**
 * H2 2.4.240 evaluates CHECK constraints on the connection that created them.
 * Hibernate adds {@code type IN (...)} checks for enums, and later inserts
 * of new enum values fail with "Check constraint violation: CONSTRAINT_4".
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
