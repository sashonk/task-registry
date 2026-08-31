package ru.asocial.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.asocial.auth.dto.AuthUserResponse;
import ru.asocial.auth.dto.LoginRequest;
import ru.asocial.auth.dto.LoginResponse;
import ru.asocial.auth.model.AppUser;
import ru.asocial.auth.security.AppUserDetailsService;
import ru.asocial.auth.security.AuthMapper;
import ru.asocial.auth.service.JwtService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final AppUserDetailsService userDetailsService;
	private final JwtService jwtService;

	public AuthController(
			AuthenticationManager authenticationManager,
			AppUserDetailsService userDetailsService,
			JwtService jwtService) {
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
		this.jwtService = jwtService;
	}

	@PostMapping("/login")
	public LoginResponse login(@RequestBody LoginRequest request) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.username(), request.password()));

		AppUser user = userDetailsService.requireUser(request.username());
		String accessToken = jwtService.issueAccessToken(user);
		AuthUserResponse userResponse = AuthMapper.toResponse(user);

		return new LoginResponse(
				accessToken,
				"Bearer",
				jwtService.accessTokenExpiresInSeconds(),
				userResponse);
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
