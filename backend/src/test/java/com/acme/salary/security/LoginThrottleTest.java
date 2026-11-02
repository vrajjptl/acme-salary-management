package com.acme.salary.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LoginThrottleTest {
    @Test
    void blocksUntilTheWindowPassesAndASuccessClearsTheCount() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-02T00:00:00Z"));
        LoginThrottle throttle = new LoginThrottle(3, Duration.ofMinutes(5), now::get);
        assertThat(throttle.allow("127.0.0.1")).isTrue();
        throttle.recordFailure("127.0.0.1");
        throttle.recordFailure("127.0.0.1");
        throttle.recordFailure("127.0.0.1");
        assertThat(throttle.allow("127.0.0.1")).isFalse();

        now.set(now.get().plus(Duration.ofMinutes(6)));
        assertThat(throttle.allow("127.0.0.1")).isTrue();

        throttle.recordFailure("10.0.0.8");
        throttle.recordSuccess("10.0.0.8");
        assertThat(throttle.allow("10.0.0.8")).isTrue();
    }
}
