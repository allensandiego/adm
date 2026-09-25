package com.allensandiego.adm.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThrottledAuthenticationProvider implements AuthenticationProvider {

    private final UserPrincipalService userPrincipalService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptRegistry loginAttemptRegistry;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String rawPassword = authentication.getCredentials().toString();

        if (loginAttemptRegistry.isThrottled(username)) {
            loginAttemptRegistry.recordFailure(username);
            throw new BadCredentialsException("Invalid username or password.");
        }

        try {
            UserDetails userDetails = userPrincipalService.loadUserByUsername(username);

            if (!userDetails.isEnabled()) {
                loginAttemptRegistry.recordFailure(username);
                throw new BadCredentialsException("Account is inactive. Please contact support.");
            }

            if (!passwordEncoder.matches(rawPassword, userDetails.getPassword())) {
                loginAttemptRegistry.recordFailure(username);
                throw new BadCredentialsException("Invalid username or password.");
            }

            loginAttemptRegistry.clear(username);

            return new UsernamePasswordAuthenticationToken(
                    userDetails.getUsername(),
                    null,
                    userDetails.getAuthorities()
            );
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            loginAttemptRegistry.recordFailure(username);
            throw new BadCredentialsException("Invalid username or password.");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
