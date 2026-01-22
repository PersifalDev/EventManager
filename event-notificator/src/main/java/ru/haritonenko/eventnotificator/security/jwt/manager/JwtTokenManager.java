package ru.haritonenko.eventnotificator.security.jwt.manager;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.securirty.user.AuthUser;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtTokenManager {

    @Value("${jwt.secret-key}")
    private String keyString;

    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(keyString.getBytes(StandardCharsets.UTF_8));
    }

    public AuthUser parseAuthUser(String jwt) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();

        Long id = claims.get("userId", Long.class);
        String login = claims.getSubject();
        String role = claims.get("role", String.class);

        return AuthUser.builder()
                .id(id)
                .login(login)
                .role(role)
                .build();
    }
}
