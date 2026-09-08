package com.speed_anwer.expensetracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "unit-test-secret-key-that-is-long-enough-for-hs256!",
                900_000L,
                604_800_000L
        );
    }

    @Test
    void generateAccessToken_containsAccessTypeAndUserId() {
        JwtService.GeneratedToken token = jwtService.generateAccessToken(42L, "user@test.com");

        Claims claims = jwtService.parseToken(token.token());
        assertThat(jwtService.isTokenType(claims, JwtService.TOKEN_TYPE_ACCESS)).isTrue();
        assertThat(jwtService.extractEmail(claims)).isEqualTo("user@test.com");
        assertThat(token.tokenId()).isNotBlank();
    }

    @Test
    void generateRefreshToken_containsRefreshType() {
        JwtService.GeneratedToken token = jwtService.generateRefreshToken(42L, "user@test.com");

        Claims claims = jwtService.parseToken(token.token());
        assertThat(jwtService.isTokenType(claims, JwtService.TOKEN_TYPE_REFRESH)).isTrue();
    }

    @Test
    void accessTokenMustNotBeAcceptedAsRefresh() {
        JwtService.GeneratedToken access = jwtService.generateAccessToken(42L, "user@test.com");

        Claims claims = jwtService.parseToken(access.token());
        assertThat(jwtService.isTokenType(claims, JwtService.TOKEN_TYPE_REFRESH)).isFalse();
    }

    @Test
    void parseToken_rejectsTamperedToken() {
        JwtService.GeneratedToken token = jwtService.generateAccessToken(42L, "user@test.com");
        String tampered = token.token().substring(0, token.token().length() - 4) + "XXXX";

        assertThatThrownBy(() -> jwtService.parseToken(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseToken_rejectsTokenSignedWithDifferentKey() {
        JwtService otherService = new JwtService(
                "another-completely-different-secret-key-32-chars!!",
                900_000L,
                604_800_000L
        );
        JwtService.GeneratedToken foreign = otherService.generateAccessToken(1L, "user@test.com");

        assertThatThrownBy(() -> jwtService.parseToken(foreign.token()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void expiredAccessToken_isRejected() {
        JwtService shortLived = new JwtService(
                "unit-test-secret-key-that-is-long-enough-for-hs256!",
                -1_000L,
                604_800_000L
        );
        JwtService.GeneratedToken token = shortLived.generateAccessToken(1L, "user@test.com");

        assertThatThrownBy(() -> shortLived.parseToken(token.token()))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void tokenIds_areUniquePerGeneration() {
        JwtService.GeneratedToken t1 = jwtService.generateAccessToken(1L, "user@test.com");
        JwtService.GeneratedToken t2 = jwtService.generateAccessToken(1L, "user@test.com");

        assertThat(t1.tokenId()).isNotEqualTo(t2.tokenId());
        assertThat(t1.token()).isNotEqualTo(t2.token());
    }
}
