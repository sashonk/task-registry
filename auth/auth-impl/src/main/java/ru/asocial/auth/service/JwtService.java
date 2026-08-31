package ru.asocial.auth.service;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import ru.asocial.auth.jwt.JobflowJwtProperties;
import ru.asocial.auth.model.AppUser;

@Service
public class JwtService {

	private final JwtEncoder jwtEncoder;
	private final JobflowJwtProperties properties;

	public JwtService(JwtEncoder jwtEncoder, JobflowJwtProperties properties) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
	}

	public String issueAccessToken(AppUser user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(properties.accessTokenTtl());

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.subject(user.getUsername())
				.issuedAt(now)
				.expiresAt(expiresAt)
				.claim("roles", List.of(user.getRole().name()))
				.build();

		JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
	}

	public long accessTokenExpiresInSeconds() {
		return properties.accessTokenTtl().getSeconds();
	}
}
