package com.allensandiego.adm.security;

import java.time.LocalDateTime;
import java.util.Deque;
import java.util.LinkedList;

/**
 * In-memory sliding window registry for tracking login attempts per username.
 */
public class LoginAttemptRegistry {

    private final int maxAttempts;
    private final java.time.Duration windowSize;
    private final java.util.concurrent.ConcurrentHashMap<String, Deque<LocalDateTime>> attempts = new java.util.concurrent.ConcurrentHashMap<>();

    public LoginAttemptRegistry(int maxAttempts, java.time.Duration windowSize) {
        this.maxAttempts = maxAttempts;
        this.windowSize = windowSize;
    }

    public boolean isThrottled(String username) {
        if (username == null) return false;
        Deque<LocalDateTime> timestamps = attempts.get(username);
        if (timestamps == null) {
            return false;
        }
        LocalDateTime cutoff = LocalDateTime.now().minus(windowSize);
        timestamps.removeIf(t -> t.isBefore(cutoff));
        return timestamps.size() >= maxAttempts;
    }

    public void recordFailure(String username) {
        if (username == null) return;
        attempts.computeIfAbsent(username, k -> new LinkedList<>()).add(LocalDateTime.now());
    }

    public void clear(String username) {
        if (username == null) return;
        attempts.remove(username);
    }
}
