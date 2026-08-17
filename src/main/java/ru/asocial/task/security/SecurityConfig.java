package ru.asocial.task.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository)
			throws Exception {
		http.csrf(csrf -> csrf.disable())
				.securityContext(context -> context.securityContextRepository(securityContextRepository))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/login.html",
								"/css/**",
								"/js/**",
								"/images/**",
								"/api/auth/login")
						.permitAll()
						.requestMatchers("/h2-console/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("ADMIN", "VIEWER")
						.requestMatchers(HttpMethod.POST, "/api/auth/logout").hasAnyRole("ADMIN", "VIEWER")
						.requestMatchers("/api/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable())
				.logout(logout -> logout.disable())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) -> {
							if (request.getRequestURI().startsWith("/api/")) {
								response.sendError(HttpStatus.UNAUTHORIZED.value());
								return;
							}
							response.sendRedirect("/login.html");
						})
						.accessDeniedHandler((request, response, accessDeniedException) -> {
							response.setStatus(HttpStatus.FORBIDDEN.value());
							response.setContentType(MediaType.APPLICATION_JSON_VALUE);
							response.getWriter().write("{\"error\":\"Недостаточно прав\"}");
						}));

		http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

		return http.build();
	}
}
