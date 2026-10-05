package com.hireflow.security;

import com.hireflow.common.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 900; // 15 minutes

    private final Map<String, Deque<Instant>> attemptCache = new ConcurrentHashMap<>();

    public void checkLoginRateLimit(String key) {
        Instant now = Instant.now();
        Deque<Instant> attempts = attemptCache.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (attempts) {
            // Remove attempts outside sliding window
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(now.minusSeconds(WINDOW_SECONDS))) {
                attempts.pollFirst();
            }

            if (attempts.size() >= MAX_ATTEMPTS) {
                throw new BadRequestException("Too many failed login attempts. Account temporarily locked. Please try again in 15 minutes.");
            }
        }
    }

    public void recordFailedAttempt(String key) {
        Deque<Instant> attempts = attemptCache.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (attempts) {
            attempts.addLast(Instant.now());
        }
    }

    public void resetAttempts(String key) {
        attemptCache.remove(key);
    }
}
