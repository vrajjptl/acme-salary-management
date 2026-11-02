package com.acme.salary.security;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class LoginThrottle {
    private final int maxFailures;
    private final Duration window;
    private final Supplier<Instant> now;
    private final ConcurrentHashMap<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    public LoginThrottle(int maxFailures, Duration window, Supplier<Instant> now) {
        if (maxFailures < 1) {
            throw new IllegalArgumentException("maxFailures must be positive");
        }
        this.maxFailures = maxFailures;
        this.window = window;
        this.now = now;
    }

    public boolean allow(String key) {
        synchronized (lock) {
            String id = normalize(key);
            prune(id, now.get());
            Deque<Instant> attempts = failures.get(id);
            return attempts == null || attempts.size() < maxFailures;
        }
    }

    public void recordFailure(String key) {
        synchronized (lock) {
            String id = normalize(key);
            Instant current = now.get();
            Deque<Instant> attempts = failures.computeIfAbsent(id, ignored -> new ArrayDeque<>());
            attempts.addLast(current);
            prune(id, current);
        }
    }

    public void recordSuccess(String key) {
        synchronized (lock) {
            failures.remove(normalize(key));
        }
    }

    public void reset() {
        synchronized (lock) {
            failures.clear();
        }
    }

    private void prune(String id, Instant current) {
        Deque<Instant> attempts = failures.get(id);
        if (attempts == null) {
            return;
        }
        Instant cutoff = current.minus(window);
        while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(cutoff)) {
            attempts.removeFirst();
        }
        if (attempts.isEmpty()) {
            failures.remove(id);
        }
    }

    private static String normalize(String key) {
        if (key == null || key.isBlank()) {
            return "unknown";
        }
        String trimmed = key.trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }
}
