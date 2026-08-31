package ru.asocial.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ru.asocial.auth.model.AppUser;
import ru.asocial.auth.model.UserRole;
import ru.asocial.auth.repository.UserRepository;

@Component
public class UserDataInitializer {

	private static final Logger log = LoggerFactory.getLogger(UserDataInitializer.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void seedUsers() {
		createUserIfMissing("admin", "admin", UserRole.ADMIN);
		createUserIfMissing("user", "user", UserRole.VIEWER);
	}

	private void createUserIfMissing(String username, String rawPassword, UserRole role) {
		if (userRepository.findByUsername(username).isPresent()) {
			return;
		}

		userRepository.save(new AppUser(username, passwordEncoder.encode(rawPassword), role));
		log.info("Created default user '{}' with role {}", username, role);
	}
}
