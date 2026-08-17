package ru.asocial.task.model;

public enum UserRole {
	ADMIN("Админ"),
	VIEWER("Просмотр");

	private final String displayName;

	UserRole(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}
}
