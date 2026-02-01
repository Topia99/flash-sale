package com.flashSale.auth.service;

import com.flashSale.auth.domain.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {
    private final JwtEncoder jwtEncoder;

    public TokenService(JwtEncoder jwtEncoder){
        this.jwtEncoder = jwtEncoder;
    }

    public Jwt generateJwt(String issuer, long expiresMinutes, Long userId, String username, String role){

        Instant now = Instant.now();
        Instant exp = now.plus(expiresMinutes, ChronoUnit.MINUTES);

        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(exp)
                .subject(String.valueOf(userId))          // sub = userId（稳定）
                .claim("username", username)
                .claim("role", role)
                .build();

        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
    }
}
