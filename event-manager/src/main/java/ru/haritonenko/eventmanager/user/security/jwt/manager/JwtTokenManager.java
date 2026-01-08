package ru.haritonenko.eventmanager.user.security.jwt.manager;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.securirty.user.AuthUser;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenManager {

    @Value("${jwt.secret-key}")
    private String keyString;

    @Value("${jwt.lifetime}")
    private long expirationTime;

    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(keyString.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Integer userId, String login, String role) {
        return Jwts
                .builder()
                .subject(login)
                .claim("userId", userId)
                .claim("role", role)
                .signWith(key)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .compact();
    }

    public AuthUser getAuthUserFromToken(String jwt) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();

        Integer userId = claims.get("userId", Integer.class);
        String login = claims.getSubject();
        String role = claims.get("role", String.class);

        return AuthUser.builder()
                .id(userId)
                .login(login)
                .role(role)
                .build();
    }
}
