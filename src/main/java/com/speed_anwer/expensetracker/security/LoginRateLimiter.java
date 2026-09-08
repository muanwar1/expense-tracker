package com.speed_anwer.expensetracker.security;

import com.speed_anwer.expensetracker.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LoginRateLimiter {

    private static final class AttemptState {
        final AtomicInteger failures = new AtomicInteger();
        volatile long windowStartMs;
        volatile long lockedUntilMs;
    }

    private final int maxAttempts;
    private final long attemptWindowMs;
    private final long lockDurationMs;
    private final ConcurrentHashMap<String, AttemptState> states = new ConcurrentHashMap<>();

    public LoginRateLimiter(@Value("${app.login.max-attempts}") int maxAttempts,
                            @Value("${app.login.attempt-window-ms}") long attemptWindowMs,
                            @Value("${app.login.lock-duration-ms}") long lockDurationMs) {
        this.maxAttempts = maxAttempts;
        this.attemptWindowMs = attemptWindowMs;
        this.lockDurationMs = lockDurationMs;
    }

    public void checkAllowed(String key) {
        AttemptState state = states.get(key);
        if (state == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (state.lockedUntilMs > now) {
            long remainingSeconds = (state.lockedUntilMs - now) / 1000;
            throw new RateLimitExceededException(
                    "Too many failed login attempts. Try again in " + remainingSeconds + " seconds.");
        }
    }

    public void recordFailure(String key) {
        long now = System.currentTimeMillis();
        AttemptState state = states.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStartMs > attemptWindowMs) {
                AttemptState fresh = new AttemptState();
                fresh.windowStartMs = now;
                return fresh;
            }
            return existing;
        });

        if (state.failures.incrementAndGet() >= maxAttempts) {
            state.lockedUntilMs = now + lockDurationMs;
        }

        evictStaleEntries(now);
    }

    public void recordSuccess(String key) {
        states.remove(key);
    }

    private void evictStaleEntries(long now) {
        if (states.size() < 10_000) {
            return;
        }
        states.entrySet().removeIf(e ->
                now - e.getValue().windowStartMs > attemptWindowMs
                        && e.getValue().lockedUntilMs < now);
    }
}
