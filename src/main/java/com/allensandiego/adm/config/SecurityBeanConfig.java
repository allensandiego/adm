package com.allensandiego.adm.config;

import com.allensandiego.adm.security.LoginAttemptRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityBeanConfig {

    @Bean
    public LoginAttemptRegistry loginAttemptRegistry() {
        return new LoginAttemptRegistry(5, java.time.Duration.ofMinutes(15));
    }
}
