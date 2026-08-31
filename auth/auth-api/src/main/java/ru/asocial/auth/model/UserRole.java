package ru.asocial.auth.model;

public enum UserRole {

	ADMIN("Администратор"),
	VIEWER("Наблюдатель"),
	PLAY("Игра");

	private final String displayName;

	UserRole(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
