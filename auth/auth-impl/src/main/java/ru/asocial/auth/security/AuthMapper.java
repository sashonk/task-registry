package ru.asocial.auth.security;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import ru.asocial.auth.dto.AuthUserResponse;
import ru.asocial.auth.model.AppUser;
import ru.asocial.auth.model.UserRole;

public final class AuthMapper {

	private AuthMapper() {
	}

	public static AuthUserResponse toResponse(AppUser user) {
		return new AuthUserResponse(
				user.getUsername(),
				user.getRole(),
				user.getRole().getDisplayName(),
				user.getRole() == UserRole.ADMIN);
	}

	public static AuthUserResponse toResponse(Authentication authentication) {
		String username = authentication.getName();
		UserRole role = resolveRole(authentication);
		return new AuthUserResponse(
				username,
				role,
				role.getDisplayName(),
				role == UserRole.ADMIN);
	}

	private static UserRole resolveRole(Authentication authentication) {
		for (GrantedAuthority authority : authentication.getAuthorities()) {
			if ("ROLE_ADMIN".equals(authority.getAuthority())) {
				return UserRole.ADMIN;
			}
			if ("ROLE_VIEWER".equals(authority.getAuthority())) {
				return UserRole.VIEWER;
			}
			if ("ROLE_PLAY".equals(authority.getAuthority())) {
				return UserRole.PLAY;
			}
		}

		if (authentication.getPrincipal() instanceof Jwt jwt) {
			List<?> roles = jwt.getClaim("roles");
			if (roles != null && roles.contains("ADMIN")) {
				return UserRole.ADMIN;
			}
			if (roles != null && roles.contains("PLAY")) {
				return UserRole.PLAY;
			}
		}

		return UserRole.VIEWER;
	}
}
