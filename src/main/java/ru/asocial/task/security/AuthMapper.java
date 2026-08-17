package ru.asocial.task.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import ru.asocial.task.dto.AuthUserResponse;
import ru.asocial.task.exception.BadRequestException;
import ru.asocial.task.model.UserRole;

public final class AuthMapper {

	private AuthMapper() {
	}

	public static AuthUserResponse toResponse(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new BadRequestException("Not authenticated");
		}

		UserRole role = resolveRole(authentication);
		return new AuthUserResponse(
				authentication.getName(),
				role,
				role.displayName(),
				role == UserRole.ADMIN);
	}

	private static UserRole resolveRole(Authentication authentication) {
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			String value = authority.getAuthority();
			if ("ROLE_ADMIN".equals(value)) {
				return UserRole.ADMIN;
			}
			if ("ROLE_VIEWER".equals(value)) {
				return UserRole.VIEWER;
			}
		}

		throw new BadRequestException("Unknown user role");
	}
}
