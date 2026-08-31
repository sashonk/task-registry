package ru.asocial.auth.dto;

import ru.asocial.auth.model.UserRole;

public record AuthUserResponse(
		String username,
		UserRole role,
		String roleDisplayName,
		boolean admin) {
}
