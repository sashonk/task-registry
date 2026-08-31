package ru.asocial.auth.jwt;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JobflowJwtProperties(String secret, String issuer, Duration accessTokenTtl) {
}
