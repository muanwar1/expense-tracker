package com.speed_anwer.expensetracker.service.impl;

import com.speed_anwer.expensetracker.dto.request.LoginRequest;
import com.speed_anwer.expensetracker.dto.response.AuthResponse;
import com.speed_anwer.expensetracker.dto.response.UserResponse;
import com.speed_anwer.expensetracker.entity.User;
import com.speed_anwer.expensetracker.exception.InvalidTokenException;
import com.speed_anwer.expensetracker.mapper.UserMapper;
import com.speed_anwer.expensetracker.repository.UserRepository;
import com.speed_anwer.expensetracker.security.JwtService;
import com.speed_anwer.expensetracker.security.LoginRateLimiter;
import com.speed_anwer.expensetracker.security.TokenRevocationService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenRevocationService tokenRevocationService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        LoginRateLimiter realRateLimiter = new LoginRateLimiter(5, 600_000L, 900_000L);
        authService = new AuthServiceImpl(
                null, // UserService not used by login/refresh paths under test
                userRepository,
                userMapper,
                jwtService,
                authenticationManager,
                tokenRevocationService,
                realRateLimiter
        );
    }

    private Claims mockClaims(String type) {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtService.parseToken("token-" + type)).thenReturn(claims);
        org.mockito.Mockito.lenient().when(jwtService.isTokenType(claims, type)).thenReturn(true);
        return claims;
    }

    @Test
    void login_success_returnsAccessAndRefreshTokens() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(7L);
        user.setEmail("user@test.com");
        UserResponse userResponse = new UserResponse();
        userResponse.setId(7L);
        userResponse.setEmail("user@test.com");
        userResponse.setId(7L);

        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        when(jwtService.generateAccessToken(7L, "user@test.com"))
                .thenReturn(new JwtService.GeneratedToken("access.jwt", "tid-a", 1L));
        when(jwtService.generateRefreshToken(7L, "user@test.com"))
                .thenReturn(new JwtService.GeneratedToken("refresh.jwt", "tid-r", 2L));

        AuthResponse response = authService.login(request, "127.0.0.1");

        assertThat(response.getAccessToken()).isEqualTo("access.jwt");
        assertThat(response.getRefreshToken()).isEqualTo("refresh.jwt");
        assertThat(response.getUser()).isEqualTo(userResponse);
    }

    @Test
    void login_badCredentials_throwsAndRecordsFailure() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);
        verify(tokenRevocationService, never()).revoke(anyString(), anyLong());
    }

    @Test
    void login_locksAfterFiveConsecutiveFailures() {
        LoginRequest request = new LoginRequest();
        request.setEmail("locked@test.com");
        request.setPassword("wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.login(request, "10.0.0.1"))
                    .isInstanceOf(BadCredentialsException.class);
        }
        // 6th attempt is blocked by the rate limiter before hitting the authentication manager
        assertThatThrownBy(() -> authService.login(request, "10.0.0.1"))
                .isInstanceOf(com.speed_anwer.expensetracker.exception.RateLimitExceededException.class);
    }

    @Test
    void refresh_rotatesTokensAndRevokesOldRefreshToken() {
        Claims claims = mockClaims(JwtService.TOKEN_TYPE_REFRESH);
        when(jwtService.extractTokenId(claims)).thenReturn("old-tid");
        when(claims.getExpiration()).thenReturn(new java.util.Date(System.currentTimeMillis() + 60_000));

        User user = new User();
        user.setId(3L);
        user.setEmail("user@test.com");
        UserResponse userResponse = new UserResponse();
        userResponse.setId(3L);
        userResponse.setEmail("user@test.com");
        when(jwtService.extractEmail(claims)).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        when(jwtService.generateAccessToken(3L, "user@test.com"))
                .thenReturn(new JwtService.GeneratedToken("new-access", "t1", 1L));
        when(jwtService.generateRefreshToken(3L, "user@test.com"))
                .thenReturn(new JwtService.GeneratedToken("new-refresh", "t2", 2L));

        AuthResponse response = authService.refresh("token-refresh");

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        verify(tokenRevocationService).revoke(eq("old-tid"), anyLong());
    }

    @Test
    void refresh_rejectsAccessTokenUsedAsRefresh() {
        Claims claims = mockClaims(JwtService.TOKEN_TYPE_ACCESS);

        assertThatThrownBy(() -> authService.refresh("token-access"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refresh_rejectsRevokedToken() {
        Claims claims = mockClaims(JwtService.TOKEN_TYPE_REFRESH);
        when(jwtService.extractTokenId(claims)).thenReturn("revoked-tid");
        when(tokenRevocationService.isRevoked("revoked-tid")).thenReturn(true);

        assertThatThrownBy(() -> authService.refresh("token-refresh"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void refresh_rejectsGarbageToken() {
        when(jwtService.parseToken("garbage")).thenThrow(io.jsonwebtoken.security.SignatureException.class);

        assertThatThrownBy(() -> authService.refresh("garbage"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void logout_revokesBothTokens() {
        Claims accessClaims = mockClaims(JwtService.TOKEN_TYPE_ACCESS);
        when(jwtService.extractTokenId(accessClaims)).thenReturn("a-tid");
        when(accessClaims.getExpiration()).thenReturn(new java.util.Date(System.currentTimeMillis() + 60_000));

        Claims refreshClaims = mockClaims(JwtService.TOKEN_TYPE_REFRESH);
        when(jwtService.extractTokenId(refreshClaims)).thenReturn("r-tid");
        when(refreshClaims.getExpiration()).thenReturn(new java.util.Date(System.currentTimeMillis() + 60_000));

        authService.logout("token-access", "token-refresh");

        verify(tokenRevocationService).revoke(eq("a-tid"), anyLong());
        verify(tokenRevocationService).revoke(eq("r-tid"), anyLong());
    }

    @Test
    void logout_toleratesExpiredAccessToken() {
        when(jwtService.parseToken("expired")).thenThrow(io.jsonwebtoken.ExpiredJwtException.class);

        assertThatCode(() -> authService.logout("expired", null))
                .doesNotThrowAnyException();
        verify(tokenRevocationService, never()).revoke(any(), anyLong());
    }
}
