package com.allensandiego.adm.config;

import com.allensandiego.adm.security.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ThrottledAuthenticationProvider throttledAuthenticationProvider;
    private final AuthEventSuccessHandler authEventSuccessHandler;
    private final AuthEventFailureHandler authEventFailureHandler;

    public SecurityConfig(ThrottledAuthenticationProvider throttledAuthenticationProvider,
                          AuthEventSuccessHandler authEventSuccessHandler,
                          AuthEventFailureHandler authEventFailureHandler) {
        this.throttledAuthenticationProvider = throttledAuthenticationProvider;
        this.authEventSuccessHandler = authEventSuccessHandler;
        this.authEventFailureHandler = authEventFailureHandler;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(com.allensandiego.adm.security.CustomPermissionEvaluator evaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(evaluator);
        return expressionHandler;
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAnonymous = auth != null && "anonymousUser".equals(auth.getPrincipal());
            if (isAnonymous || auth == null || !auth.isAuthenticated()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
        };
    }

    @Bean
    public LogoutHandler authEventLogoutHandler(com.allensandiego.adm.service.AuthEventService authEventService) {
        return (request, response, authentication) -> {
            if (authentication != null && authentication.isAuthenticated()) {
                com.allensandiego.adm.domain.AuthEvent event = new com.allensandiego.adm.domain.AuthEvent();
                event.setUsername(authentication.getName());
                event.setOutcome(com.allensandiego.adm.domain.AuthOutcome.SUCCESS);
                event.setIpAddress(getClientIp(request));
                event.setUserAgent(request.getHeader("User-Agent"));
                authEventService.record(event);
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AccessDeniedHandler accessDeniedHandler,
                                                   LogoutHandler logoutHandler) throws Exception {
        System.out.println("=== SecurityConfig.securityFilterChain ===");
        
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**", "/assets/**", "/css/**", "/js/**", "/webjars/**", "/favicon.ico", "/error", "/login").permitAll()
                .requestMatchers("/users/**", "/roles/**", "/permissions/**", "/audit/**", "/api/admin/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler))
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(authEventSuccessHandler)
                .failureHandler(authEventFailureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(PathPatternRequestMatcher.pathPattern("/logout"))
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        // Configure the authentication provider for form login
        http.authenticationProvider(throttledAuthenticationProvider);

        var filterChain = http.build();
        System.out.println("FilterChain built: " + filterChain);
        return filterChain;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
