package com.speed_anwer.expensetracker.service.impl;

import com.speed_anwer.expensetracker.dto.request.LoginRequest;
import com.speed_anwer.expensetracker.dto.request.RegisterRequest;
import com.speed_anwer.expensetracker.dto.response.AuthResponse;
import com.speed_anwer.expensetracker.dto.response.UserResponse;
import com.speed_anwer.expensetracker.exception.InvalidTokenException;
import com.speed_anwer.expensetracker.mapper.UserMapper;
import com.speed_anwer.expensetracker.repository.UserRepository;
import com.speed_anwer.expensetracker.security.JwtService;
import com.speed_anwer.expensetracker.security.LoginRateLimiter;
import com.speed_anwer.expensetracker.security.TokenRevocationService;
import com.speed_anwer.expensetracker.service.interfaces.AuthService;
import com.speed_anwer.expensetracker.service.interfaces.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenRevocationService tokenRevocationService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthServiceImpl(UserService userService,
                           UserRepository userRepository,
                           UserMapper userMapper,
                           JwtService jwtService,
                           AuthenticationManager authenticationManager,
                           TokenRevocationService tokenRevocationService,
                           LoginRateLimiter loginRateLimiter) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.tokenRevocationService = tokenRevocationService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        UserResponse user = userService.register(request);
        return buildAuthResponse(user.getId(), user.getEmail(), user);
    }

    @Override
    public AuthResponse login(LoginRequest request, String clientIp) {
        String rateLimitKey = clientIp + ":" + request.getEmail().toLowerCase();
        loginRateLimiter.checkAllowed(rateLimitKey);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            loginRateLimiter.recordFailure(rateLimitKey);
            throw e;
        }

        loginRateLimiter.recordSuccess(rateLimitKey);

        UserResponse user = userRepository.findByEmail(request.getEmail())
                .map(userMapper::toResponse)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        return buildAuthResponse(user.getId(), user.getEmail(), user);
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        Claims claims = parseAndValidate(refreshToken);

        if (!jwtService.isTokenType(claims, JwtService.TOKEN_TYPE_REFRESH)) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        if (tokenRevocationService.isRevoked(jwtService.extractTokenId(claims))) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        String email = jwtService.extractEmail(claims);
        UserResponse user = userRepository.findByEmail(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        // rotate: revoke the used refresh token so it cannot be reused
        tokenRevocationService.revoke(jwtService.extractTokenId(claims), claims.getExpiration().getTime());

        return buildAuthResponse(user.getId(), user.getEmail(), user);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        revokeQuietly(accessToken);

        if (refreshToken != null && !refreshToken.isBlank()) {
            revokeQuietly(refreshToken);
        }
    }

    private void revokeQuietly(String token) {
        try {
            Claims claims = jwtService.parseToken(token);
            tokenRevocationService.revoke(jwtService.extractTokenId(claims), claims.getExpiration().getTime());
        } catch (JwtException | IllegalArgumentException ignored) {
            // already expired or malformed - nothing to revoke
        }
    }

    private AuthResponse buildAuthResponse(Long userId, String email, UserResponse user) {
        JwtService.GeneratedToken access = jwtService.generateAccessToken(userId, email);
        JwtService.GeneratedToken refresh = jwtService.generateRefreshToken(userId, email);
        return new AuthResponse(access.token(), refresh.token(), "Bearer", user);
    }

    private Claims parseAndValidate(String token) {
        try {
            return jwtService.parseToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid or expired token");
        }
    }
}
