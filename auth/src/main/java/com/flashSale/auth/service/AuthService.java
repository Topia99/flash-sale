package com.flashSale.auth.service;

import com.flashSale.auth.domain.User;
import com.flashSale.auth.dto.LoginRequest;
import com.flashSale.auth.dto.RegisterRequest;
import com.flashSale.auth.exception.ConflictException;
import com.flashSale.auth.exception.UnauthorizedException;
import com.flashSale.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;

    private final String issuer;
    private final long expiresMinutes;

    public AuthService(
            UserRepository users,
            PasswordEncoder encoder,
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expires-minutes}") long expiresMinutes){
        this.users = users;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expiresMinutes = expiresMinutes;
    }

    @Transactional
    public User register(RegisterRequest req) {
        // 先做快速存在性检查（更友好错误信息）
        if (users.existsByUsername(req.username())) {
            throw new ConflictException("USERNAME_TAKEN", "username already exists");
        }
        if (req.email() != null && !req.email().isBlank() && users.existsByEmail(req.email())) {
            throw new ConflictException("EMAIL_TAKEN", "email already exists");
        }

        var u = new User();
        u.setUsername(req.username());
        u.setEmail((req.email() == null || req.email().isBlank()) ? null : req.email());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole("USER");
        u.setStatus((byte) 1);

        try {
            return users.save(u);
        } catch (DataIntegrityViolationException e) {
            // 防并发双写：最终仍以 DB unique 为准
            throw new ConflictException("DUPLICATE", "duplicate username/email");
        }
    }

    public Jwt login(LoginRequest req) {
        var user = users.findByUsername(req.username())
                .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", "invalid username or password"));

        if (user.getStatus() == 0) {
            throw new UnauthorizedException("USER_DISABLED", "user is disabled");
        }

        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "invalid username or password");
        }

        Instant now = Instant.now();
        Instant exp = now.plus(expiresMinutes, ChronoUnit.MINUTES);

        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(exp)
                .subject(String.valueOf(user.getId()))          // sub = userId（稳定）
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .build();

        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
    }

}
