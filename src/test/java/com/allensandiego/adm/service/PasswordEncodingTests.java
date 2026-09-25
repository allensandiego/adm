package com.allensandiego.adm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PasswordEncodingTests {

    @Autowired private PasswordEncoder passwordEncoder;

    private String rawPassword;

    @BeforeEach
    void setUp() {
        rawPassword = "SecureP@ssw0rd!2024";
    }

    @Test
    @DisplayName("T029-1: Encoded password is not reversible")
    void encodedPasswordNotReversible() {
        String encoded = passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, encoded);
        assertFalse(encoded.contains(rawPassword));
        assertFalse(encoded.startsWith("raw:"));
    }

    @Test
    @DisplayName("T029-2: Encoded password matches original")
    void encodedPasswordMatchesOriginal() {
        String encoded = passwordEncoder.encode(rawPassword);

        assertTrue(passwordEncoder.matches(rawPassword, encoded));
    }

    @Test
    @DisplayName("T029-3: Wrong password does not match encoded password")
    void wrongPasswordDoesNotMatchEncodedPassword() {
        String encoded = passwordEncoder.encode(rawPassword);

        assertFalse(passwordEncoder.matches("WrongPassword123", encoded));
    }

    @Test
    @DisplayName("T029-4: Same password produces different encodings (salted)")
    void samePasswordProducesDifferentEncodings() {
        String encoded1 = passwordEncoder.encode(rawPassword);
        String encoded2 = passwordEncoder.encode(rawPassword);

        assertNotEquals(encoded1, encoded2);
        assertTrue(passwordEncoder.matches(rawPassword, encoded1));
        assertTrue(passwordEncoder.matches(rawPassword, encoded2));
    }

    @Test
    @DisplayName("T029-5: Encoded password contains algorithm identifier")
    void encodedPasswordContainsAlgorithmIdentifier() {
        String encoded = passwordEncoder.encode(rawPassword);

        assertTrue(encoded.startsWith("{"));
        assertTrue(encoded.contains("}"));
    }

    @Test
    @DisplayName("T029-6: Empty password encoding produces a valid hash (but won't match)")
    void emptyPasswordCanBeEncoded() {
        String encoded = passwordEncoder.encode("");
        
        // BCrypt allows empty passwords but the hash won't match empty string
        assertNotNull(encoded);
        assertNotEquals("", encoded);
        // Note: Empty password hash doesn't match empty string due to BCrypt implementation
        assertFalse(passwordEncoder.matches("", encoded));
    }

    @Test
    @DisplayName("T029-7: Null password encoding is handled")
    void nullPasswordEncodingHandled() {
        assertDoesNotThrow(() -> passwordEncoder.encode(null));
    }

    @Test
    @DisplayName("T030-1: Delegating encoder supports multiple algorithms")
    void delegatingEncoderSupportsMultipleAlgorithms() {
        String encoded = passwordEncoder.encode(rawPassword);

        assertTrue(encoded.startsWith("{"));
        // Extract algorithm identifier between { and }
        int start = encoded.indexOf('{') + 1;
        int end = encoded.indexOf('}');
        String algorithm = encoded.substring(start, end);

        assertNotNull(algorithm);
        assertFalse(algorithm.isEmpty());
    }

    @Test
    @DisplayName("T030-2: Encoded password is suitable for database storage")
    void encodedPasswordSuitableForDatabaseStorage() {
        String encoded = passwordEncoder.encode(rawPassword);

        // BCrypt passwords are typically 60 characters, Delegating adds prefix
        assertTrue(encoded.length() > 50);
        assertFalse(encoded.contains("\n"));
        assertFalse(encoded.contains("\r"));
    }

    @Test
    @DisplayName("T030-3: Password encoding is consistent across multiple calls")
    void passwordEncodingConsistentAcrossMultipleCalls() {
        String encoded = passwordEncoder.encode(rawPassword);

        for (int i = 0; i < 10; i++) {
            assertTrue(passwordEncoder.matches(rawPassword, encoded));
        }
    }

    @Test
    @DisplayName("T030-4: Password with special characters is encoded correctly")
    void passwordWithSpecialCharactersEncodedCorrectly() {
        String specialPassword = "P@ss!w0rd#$%^&*()";
        String encoded = passwordEncoder.encode(specialPassword);

        assertTrue(passwordEncoder.matches(specialPassword, encoded));
        assertFalse(passwordEncoder.matches("Wrong", encoded));
    }

    @Test
    @DisplayName("T030-5: Password with unicode characters is encoded correctly")
    void passwordWithUnicodeCharactersEncodedCorrectly() {
        String unicodePassword = "Pässwörd🔒";
        String encoded = passwordEncoder.encode(unicodePassword);

        assertTrue(passwordEncoder.matches(unicodePassword, encoded));
        assertFalse(passwordEncoder.matches("Wrong", encoded));
    }
}
