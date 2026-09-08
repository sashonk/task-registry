package ru.asocial.auth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import ru.asocial.auth.dto.AuthUserResponse;
import ru.asocial.auth.dto.LoginRequest;
import ru.asocial.auth.dto.LoginResponse;
import ru.asocial.auth.model.AppUser;
import ru.asocial.auth.security.AppUserDetailsService;
import ru.asocial.auth.security.AuthMapper;
import ru.asocial.auth.service.AuditEventProducer;
import ru.asocial.auth.service.JwtService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private final AuthenticationManager authenticationManager;
	private final AppUserDetailsService userDetailsService;
	private final JwtService jwtService;
	private final AuditEventProducer auditProducer;

	public AuthController(
			AuthenticationManager authenticationManager,
			AppUserDetailsService userDetailsService,
			JwtService jwtService,
			AuditEventProducer auditProducer) {
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
		this.jwtService = jwtService;
		this.auditProducer = auditProducer;
	}

	@PostMapping("/login")
	public LoginResponse login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.username(), request.password()));

		AppUser user = userDetailsService.requireUser(request.username());
		String accessToken = jwtService.issueAccessToken(user);
		AuthUserResponse userResponse = AuthMapper.toResponse(user);

		try {
			auditProducer.sendLoginEvent(
					user.getUsername(),
					extractIpAddress(httpRequest),
					user.getRole().name());
		} catch (Exception e) {
			log.warn("Failed to send audit event for login: {}", user.getUsername(), e);
		}

		return new LoginResponse(
				accessToken,
				"Bearer",
				jwtService.accessTokenExpiresInSeconds(),
				userResponse);
	}

	private String extractIpAddress(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isEmpty()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	@GetMapping("/me")
	public AuthUserResponse currentUser(Authentication authentication) {
		return AuthMapper.toResponse(authentication);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		return ResponseEntity.noContent().build();
	}
}
