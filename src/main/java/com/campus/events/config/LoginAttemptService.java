package com.campus.events.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private static class Attempt {
        int count;
        LocalDateTime lastAttempt;
        LocalDateTime lockedUntil;

        Attempt(int count, LocalDateTime lastAttempt) {
            this.count = count;
            this.lastAttempt = lastAttempt;
        }
    }

    private final Map<String, Attempt> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        attemptsCache.remove(key.toLowerCase());
    }

    public void loginFailed(String key) {
        String normalized = key.toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        attemptsCache.compute(normalized, (k, existing) -> {
            if (existing == null) {
                return new Attempt(1, now);
            }
            if (existing.lockedUntil != null && now.isAfter(existing.lockedUntil)) {
                // Lockout period passed, reset
                return new Attempt(1, now);
            }
            existing.count++;
            existing.lastAttempt = now;
            if (existing.count >= MAX_ATTEMPTS) {
                existing.lockedUntil = now.plusMinutes(LOCKOUT_MINUTES);
                log.warn("Account/IP '{}' locked out for {} minutes after {} failed attempts.", normalized, LOCKOUT_MINUTES, existing.count);
            }
            return existing;
        });
    }

    public boolean isBlocked(String key) {
        String normalized = key.toLowerCase();
        Attempt attempt = attemptsCache.get(normalized);
        if (attempt == null) {
            return false;
        }
        if (attempt.lockedUntil != null) {
            if (LocalDateTime.now().isBefore(attempt.lockedUntil)) {
                return true;
            } else {
                // Lock expired
                attemptsCache.remove(normalized);
                return false;
            }
        }
        return false;
    }

    public int getRemainingAttempts(String key) {
        String normalized = key.toLowerCase();
        Attempt attempt = attemptsCache.get(normalized);
        if (attempt == null) {
            return MAX_ATTEMPTS;
        }
        return Math.max(0, MAX_ATTEMPTS - attempt.count);
    }
}

