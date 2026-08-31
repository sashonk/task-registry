package ru.asocial.auth.jwt;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@Configuration
@EnableConfigurationProperties(JobflowJwtProperties.class)
public class JobflowJwtAutoConfiguration {

	@Bean
	JwtDecoder jwtDecoder(JobflowJwtProperties properties) {
		return JobflowJwtSupport.jwtDecoder(properties);
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		return JobflowJwtSupport.jwtAuthenticationConverter();
	}
}
