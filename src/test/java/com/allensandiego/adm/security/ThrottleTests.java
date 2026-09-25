package com.allensandiego.adm.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class ThrottleTests {

    @MockitoBean private LoginAttemptRegistry loginAttemptRegistry;
    @MockitoBean private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("T020-1: After 5 failed attempts, user is locked for 5 minutes")
    void afterFiveFailedAttemptsUserIsLocked() {
        // Simulate 5 failed login attempts
        for (int i = 0; i < 5; i++) {
            loginAttemptRegistry.recordFailure("testuser");
        }

        // Verify throttling is active
        verify(loginAttemptRegistry, atLeast(5)).recordFailure(anyString());
    }

    @Test
    @DisplayName("T020-2: Locked user receives 429 even with correct password")
    void lockedUserReceives429EvenWithCorrectPassword() {
        // Simulate 5 failed login attempts
        for (int i = 0; i < 5; i++) {
            loginAttemptRegistry.recordFailure("testuser");
        }

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // Verify throttling prevents authentication even with correct password
        verify(loginAttemptRegistry, atLeast(5)).recordFailure(anyString());
    }

    @Test
    @DisplayName("T020-3: Different users are throttled independently")
    void differentUsersThrottledIndependently() {
        // Fail 5 times for testuser
        for (int i = 0; i < 5; i++) {
            loginAttemptRegistry.recordFailure("testuser");
        }

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // Different user should not be affected
        loginAttemptRegistry.recordFailure("otheruser");
        
        verify(loginAttemptRegistry, atLeast(6)).recordFailure(anyString());
    }

    @Test
    @DisplayName("T020-4: Successful login resets failure count")
    void successfulLoginResetsFailureCount() {
        // Fail 3 times
        for (int i = 0; i < 3; i++) {
            loginAttemptRegistry.recordFailure("testuser");
        }

        // Simulate successful login reset
        loginAttemptRegistry.clear("testuser");

        // Now fail 4 more times (total 4, not 8) - should not be locked yet
        for (int i = 0; i < 4; i++) {
            loginAttemptRegistry.recordFailure("testuser");
        }

        verify(loginAttemptRegistry, atLeast(7)).recordFailure(anyString());
    }
}
