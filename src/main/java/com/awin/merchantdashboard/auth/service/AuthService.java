package com.awin.merchantdashboard.auth.service;

import com.awin.merchantdashboard.api.model.TokenResponse;
import com.awin.merchantdashboard.auth.model.AppUser;
import com.awin.merchantdashboard.auth.repository.AppUserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    static final Duration TOKEN_TTL = Duration.ofHours(1);
    private static final String TOKEN_TYPE = "Bearer";

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
    }

    public TokenResponse issueToken(String username, String password) {
        AppUser user = users.findByUsername(username).orElseThrow(this::unauthorized);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw unauthorized();
        }
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(Long.toString(user.getId()))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(TOKEN_TTL))
                .build();
        String accessToken =
                jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(accessToken, TOKEN_TYPE, TOKEN_TTL.toSeconds());
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
}
