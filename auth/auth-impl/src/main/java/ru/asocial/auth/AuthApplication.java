package ru.asocial.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import ru.asocial.auth.jwt.JobflowJwtAutoConfiguration;

@SpringBootApplication
@Import(JobflowJwtAutoConfiguration.class)
public class AuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthApplication.class, args);
	}
}
