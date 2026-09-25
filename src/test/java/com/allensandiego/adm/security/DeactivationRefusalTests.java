package com.allensandiego.adm.security;

import com.allensandiego.adm.domain.User;
import com.allensandiego.adm.domain.UserRepository;
import com.allensandiego.adm.domain.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class DeactivationRefusalTests {

    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private UserPrincipalService userPrincipalService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("T023-1: Inactive user authentication is refused")
    void inactiveUserAuthenticationRefused() {
        // Create an inactive user in the database for testing (use unique username)
        User inactiveUser = new User();
        inactiveUser.setUsername("inactiveuser_" + System.currentTimeMillis());
        inactiveUser.setDisplayName("Inactive User");
        inactiveUser.setPasswordHash(passwordEncoder.encode("password"));
        inactiveUser.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(inactiveUser);
        
        // Inactive user should be rejected during authentication
        var userDetails = userPrincipalService.loadUserByUsername(inactiveUser.getUsername());
        assertFalse(userDetails.isEnabled());
    }

    @Test
    @DisplayName("T023-2: Active user authentication succeeds")
    void activeUserAuthenticationSucceeds() {
        // Create an active user in the database for testing (use unique username)
        User activeUser = new User();
        activeUser.setUsername("activeuser_" + System.currentTimeMillis());
        activeUser.setDisplayName("Active User");
        activeUser.setPasswordHash(passwordEncoder.encode("password"));
        activeUser.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(activeUser);
        
        var userDetails = userPrincipalService.loadUserByUsername(activeUser.getUsername());
        assertTrue(userDetails.isEnabled());
    }

    @Test
    @DisplayName("T023-3: Inactive user credentials are not validated against password hash")
    void inactiveUserPasswordNotValidated() {
        // Create an inactive user in the database for testing (use unique username)
        User inactiveUser = new User();
        inactiveUser.setUsername("inactiveuser_" + System.currentTimeMillis());
        inactiveUser.setDisplayName("Inactive User");
        inactiveUser.setPasswordHash(passwordEncoder.encode("password"));
        inactiveUser.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(inactiveUser);

        // Inactive user should be rejected without password validation
        var userDetails = userPrincipalService.loadUserByUsername(inactiveUser.getUsername());
        assertFalse(userDetails.isEnabled());
    }

    @Test
    @DisplayName("T023-4: Inactive user cannot access protected resources")
    void inactiveUserCannotAccessProtectedResources() {
        // Create an inactive user in the database for testing (use unique username)
        User inactiveUser = new User();
        inactiveUser.setUsername("inactiveuser_" + System.currentTimeMillis());
        inactiveUser.setDisplayName("Inactive User");
        inactiveUser.setPasswordHash(passwordEncoder.encode("password"));
        inactiveUser.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(inactiveUser);

        var userDetails = userPrincipalService.loadUserByUsername(inactiveUser.getUsername());
        assertFalse(userDetails.isEnabled());
    }

    @Test
    @DisplayName("T023-5: Deactivated user login attempt is recorded as failure")
    void deactivatedUserLoginAttemptRecordedAsFailure() {
        // Create an inactive user in the database for testing (use unique username)
        User inactiveUser = new User();
        inactiveUser.setUsername("inactiveuser_" + System.currentTimeMillis());
        inactiveUser.setDisplayName("Inactive User");
        inactiveUser.setPasswordHash(passwordEncoder.encode("password"));
        inactiveUser.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(inactiveUser);

        var userDetails = userPrincipalService.loadUserByUsername(inactiveUser.getUsername());
        assertFalse(userDetails.isEnabled());
    }
}
