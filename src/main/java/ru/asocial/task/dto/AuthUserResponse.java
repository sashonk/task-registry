package ru.asocial.task.dto;

import ru.asocial.task.model.UserRole;

public record AuthUserResponse(
		String username,
		UserRole role,
		String roleDisplayName,
		boolean admin) {
}
