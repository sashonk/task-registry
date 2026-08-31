package ru.asocial.scheduler.validation;

import ru.asocial.scheduler.exception.BadRequestException;

final class SqlGuard {

	private SqlGuard() {
	}

	static void validateSelect(String sql) {
		String normalized = sql.strip().toUpperCase();
		if (!normalized.startsWith("SELECT")) {
			throw new BadRequestException("Only SELECT queries are allowed");
		}
		if (normalized.contains(";") && normalized.indexOf(';') < normalized.length() - 1) {
			throw new BadRequestException("Multiple SQL statements are not allowed");
		}
		for (String forbidden : new String[] { "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE", "TRUNCATE", "MERGE" }) {
			if (normalized.contains(forbidden)) {
				throw new BadRequestException("Forbidden SQL keyword: " + forbidden);
			}
		}
	}
}
