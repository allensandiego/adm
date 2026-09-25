package com.allensandiego.adm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use BCrypt with strength 10 to match the seeded hash format ($2a$10$...)
        return new BCryptPasswordEncoder(10);
    }
}
