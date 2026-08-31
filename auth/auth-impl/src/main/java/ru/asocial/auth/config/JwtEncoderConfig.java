package ru.asocial.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import ru.asocial.auth.jwt.JobflowJwtProperties;
import ru.asocial.auth.jwt.JobflowJwtSupport;

@Configuration
public class JwtEncoderConfig {

	@Bean
	JwtEncoder jwtEncoder(JobflowJwtProperties properties) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(JobflowJwtSupport.secretKey(properties.secret())));
	}
}
