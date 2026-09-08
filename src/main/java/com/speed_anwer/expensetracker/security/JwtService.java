package com.speed_anwer.expensetracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String CLAIM_USER_ID = "userId";

    private final SecretKey signingKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
                      @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public GeneratedToken generateAccessToken(Long userId, String email) {
        return generate(userId, email, TOKEN_TYPE_ACCESS, accessExpirationMs);
    }

    public GeneratedToken generateRefreshToken(Long userId, String email) {
        return generate(userId, email, TOKEN_TYPE_REFRESH, refreshExpirationMs);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenType(Claims claims, String expectedType) {
        return expectedType.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public String extractEmail(Claims claims) {
        return claims.getSubject();
    }

    public String extractTokenId(Claims claims) {
        return claims.getId();
    }

    private GeneratedToken generate(Long userId, String email, String tokenType, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        String tokenId = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .id(tokenId)
                .subject(email)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();

        return new GeneratedToken(token, tokenId, expiry.getTime());
    }

    public record GeneratedToken(String token, String tokenId, long expiresAtMs) {
    }
}
