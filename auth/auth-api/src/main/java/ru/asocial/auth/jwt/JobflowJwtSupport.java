package ru.asocial.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

public final class JobflowJwtSupport {

	private JobflowJwtSupport() {
	}

	public static SecretKey secretKey(String secret) {
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	public static JwtDecoder jwtDecoder(JobflowJwtProperties properties) {
		return NimbusJwtDecoder.withSecretKey(secretKey(properties.secret()))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
	}

	public static JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName("roles");
		authoritiesConverter.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return converter;
	}

	public static String createAccessToken(JobflowJwtProperties properties, String username, String role) {
		JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties.secret())));
		Instant now = Instant.now();
		Instant expiresAt = now.plus(properties.accessTokenTtl());

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.subject(username)
				.issuedAt(now)
				.expiresAt(expiresAt)
				.claim("roles", List.of(role))
				.build();

		JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
	}
}
