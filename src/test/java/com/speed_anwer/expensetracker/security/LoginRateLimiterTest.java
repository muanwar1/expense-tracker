package com.speed_anwer.expensetracker.security;

import com.speed_anwer.expensetracker.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

    private LoginRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new LoginRateLimiter(3, 600_000L, 900_000L);
    }

    @Test
    void allowsFailuresBelowThreshold() {
        rateLimiter.recordFailure("ip:user@test.com");
        rateLimiter.recordFailure("ip:user@test.com");

        assertThatCode(() -> rateLimiter.checkAllowed("ip:user@test.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void locksAccountAfterMaxFailures() {
        String key = "ip:victim@test.com";
        for (int i = 0; i < 3; i++) {
            rateLimiter.recordFailure(key);
        }

        assertThatThrownBy(() -> rateLimiter.checkAllowed(key))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Too many failed login attempts");
    }

    @Test
    void lockIsIsolatedPerKey() {
        String lockedKey = "ip:locked@test.com";
        String freeKey = "ip:free@test.com";
        for (int i = 0; i < 3; i++) {
            rateLimiter.recordFailure(lockedKey);
        }

        assertThatCode(() -> rateLimiter.checkAllowed(freeKey))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> rateLimiter.checkAllowed(lockedKey))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void recordSuccess_clearsTheCounter() {
        String key = "ip:recovering@test.com";
        rateLimiter.recordFailure(key);
        rateLimiter.recordFailure(key);
        rateLimiter.recordSuccess(key);

        // after a successful login the counter resets, so two more failures must NOT lock
        rateLimiter.recordFailure(key);
        rateLimiter.recordFailure(key);

        assertThatCode(() -> rateLimiter.checkAllowed(key))
                .doesNotThrowAnyException();
    }

    @Test
    void failuresInDifferentWindows_doNotAccumulate() {
        LoginRateLimiter tinyWindow = new LoginRateLimiter(2, 1L, 900_000L);
        String key = "ip:window@test.com";

        tinyWindow.recordFailure(key);
        try {
            Thread.sleep(20);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        tinyWindow.recordFailure(key);

        // second failure started a fresh window, so still below threshold of 2 within one window
        assertThatCode(() -> tinyWindow.checkAllowed(key))
                .doesNotThrowAnyException();
    }
}
