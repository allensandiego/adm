package com.allensandiego.adm.web;

import com.allensandiego.adm.domain.Permission;
import com.allensandiego.adm.domain.PermissionRepository;
import com.allensandiego.adm.domain.User;
import com.allensandiego.adm.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final PermissionRepository permissionRepository;

    public AdminController(UserService userService, PermissionRepository permissionRepository) {
        this.userService = userService;
        this.permissionRepository = permissionRepository;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public List<User> listAdminUsers() {
        return userService.listUsersAsEntities(org.springframework.data.domain.Pageable.unpaged(), null).getContent();
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public List<Permission> listAdminPermissions() {
        return permissionRepository.findAll();
    }
}
