package com.allensandiego.adm.security;

import com.allensandiego.adm.domain.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@Service
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final PermissionResolver permissionResolver;
    private final UserRepository userRepository;

    public CustomPermissionEvaluator(PermissionResolver permissionResolver, UserRepository userRepository) {
        this.permissionResolver = permissionResolver;
        this.userRepository = userRepository;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object targetContext) {
        try {
            System.out.println("=== CustomPermissionEvaluator.hasPermission(Object, Object) ===");
            System.out.println("Auth: " + authentication);
            System.out.println("targetDomainObject: " + targetDomainObject + " (type: " + (targetDomainObject == null ? "null" : targetDomainObject.getClass().getName()) + ")");
            System.out.println("targetContext: " + targetContext);
            
            if (authentication == null || !authentication.isAuthenticated()) {
                System.out.println("=== CustomPermissionEvaluator: auth null or not authenticated ===");
                return false;
            }
            String permission;
            if (targetDomainObject instanceof String s) {
                permission = s;
            } else if (targetDomainObject != null) {
                permission = targetDomainObject.toString();
            } else if (targetContext instanceof String s) {
                System.out.println("=== CustomPermissionEvaluator: using targetContext as permission ===");
                permission = s;
            } else {
                System.out.println("=== CustomPermissionEvaluator: targetDomainObject is null ===");
                return false;
            }
            String username = extractUsername(authentication);
            System.out.println("Permission: " + permission);
            System.out.println("Extracted Username: " + username);
            if (username == null) {
                return false;
            }
            final String perm = permission;
            return userRepository.findByUsername(username)
                    .map(user -> {
                        boolean hasPerm = permissionResolver.hasPermission(user.getId(), perm);
                        System.out.println("User ID: " + user.getId() + ", Has Permission: " + hasPerm);
                        return hasPerm;
                    })
                    .orElse(false);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object targetContext) {
        try {
            System.out.println("=== CustomPermissionEvaluator.hasPermission(Serializable, String, Object) ===");
            System.out.println("Auth: " + authentication);
            System.out.println("targetId: " + targetId);
            System.out.println("targetType: " + targetType);
            System.out.println("targetContext: " + targetContext);
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return false;
            }
            String permission = targetType;
            String username = extractUsername(authentication);
            System.out.println("Permission: " + permission);
            System.out.println("Extracted Username: " + username);
            if (username == null) {
                return false;
            }
            final String perm = permission;
            return userRepository.findByUsername(username)
                    .map(user -> {
                        boolean hasPerm = permissionResolver.hasPermission(user.getId(), perm);
                        System.out.println("User ID: " + user.getId() + ", Has Permission: " + hasPerm);
                        return hasPerm;
                    })
                    .orElse(false);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String extractUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        System.out.println("extractUsername called, principal type: " + (principal == null ? "null" : principal.getClass().getName()));
        if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            System.out.println("Extracted from UserDetails: " + username);
            return username;
        } else if (principal instanceof String s) {
            System.out.println("Extracted from String: " + s);
            return s;
        } else if (principal instanceof com.allensandiego.adm.domain.User userEntity) {
            String username = userEntity.getUsername();
            System.out.println("Extracted from User entity: " + username);
            return username;
        } else if (principal != null) {
            String str = principal.toString();
            System.out.println("Extracted from toString: " + str);
            return str;
        }
        return null;
    }
}
