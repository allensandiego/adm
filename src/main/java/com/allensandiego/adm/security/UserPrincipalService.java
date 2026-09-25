package com.allensandiego.adm.security;

import com.allensandiego.adm.domain.User;
import com.allensandiego.adm.domain.UserRepository;
import com.allensandiego.adm.domain.UserRole;
import com.allensandiego.adm.domain.UserRoleRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
public class UserPrincipalService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserPrincipalService(UserRepository userRepository,
                                UserRoleRepository userRoleRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Collection<GrantedAuthority> authorities = userRoleRepository.findByUserId(user.getId()).stream()
                .filter(ur -> ur.getRole() != null && ur.getRole().isProtected())
                .map(ur -> new SimpleGrantedAuthority("ROLE_" + ur.getRole().getName()))
                .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash() != null ? user.getPasswordHash() : "",
                user.getStatus() == User.UserStatus.ACTIVE,
                true, true, true,
                authorities
        );
    }

    public boolean encodeAndStorePassword(User user, String rawPassword) {
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return true;
    }

    public boolean matches(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash() != null ? user.getPasswordHash() : "");
    }
}
