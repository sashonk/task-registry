package ru.asocial.task.service.task;

import ru.asocial.task.exception.BadRequestException;

public final class TaskSqlGuard {

	private TaskSqlGuard() {
	}

	public static void validateSelect(String sql) {
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

	public static String withLimit(String sql, int limit) {
		String trimmed = sql.strip();
		if (trimmed.endsWith(";")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1).strip();
		}
		if (trimmed.toUpperCase().contains(" LIMIT ")) {
			return trimmed;
		}
		return trimmed + " LIMIT " + limit;
	}
}
