package com.allensandiego.adm.web;

import com.allensandiego.adm.domain.Role;
import com.allensandiego.adm.dto.RoleCreateRequest;
import com.allensandiego.adm.service.AuditService;
import com.allensandiego.adm.service.PermissionService;
import com.allensandiego.adm.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;
import java.util.UUID;

@Controller
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public RoleController(RoleService roleService,
                          PermissionService permissionService,
                          AuditService auditService) {
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasPermission(#authentication, 'role.view')")
    public String listRoles(@RequestParam(required = false) String search,
                            @PageableDefault(size = 20) Pageable pageable,
                            Model model) {
        Page<Role> roles = roleService.listRolesAsEntities(pageable, search);
        model.addAttribute("pageTitle", "Roles");
        model.addAttribute("roles", roles);
        model.addAttribute("search", search);
        return "role/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasPermission(#authentication, 'role.create')")
    public String newRoleForm(Model model) {
        model.addAttribute("form", new RoleCreateRequest());
        return "role/form";
    }

    @PostMapping
    @PreAuthorize("hasPermission(#authentication, 'role.create')")
    public String createRole(@Valid @ModelAttribute RoleCreateRequest form,
                             BindingResult result,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "role/form";
        }
        try {
            String username = (auth != null) ? auth.getName() : SecurityContextHolder.getContext().getAuthentication().getName();
            roleService.createRole(form, username);
            redirectAttributes.addFlashAttribute("success", "Role created successfully.");
        } catch (IllegalArgumentException e) {
            result.rejectValue("name", "error.role", e.getMessage());
            return "role/form";
        }
        return "redirect:/roles";
    }

    @GetMapping("/{roleId}")
    @PreAuthorize("hasPermission(#authentication, 'role.view')")
    public String getRole(@PathVariable UUID roleId, Model model) {
        Role role = roleService.getRoleEntity(roleId);
        model.addAttribute("pageTitle", "Role Detail");
        model.addAttribute("role", role);
        model.addAttribute("permissions", permissionService.listPermissionsAsEntities(org.springframework.data.domain.Pageable.unpaged(), null));
        return "role/detail";
    }

    @PostMapping("/{roleId}/edit")
    @PreAuthorize("hasPermission(#authentication, 'role.edit')")
    public String editRole(@PathVariable UUID roleId,
                           Authentication auth,
                           RedirectAttributes redirectAttributes) {
        try {
            Role role = roleService.getRoleEntity(roleId);
            String username = (auth != null) ? auth.getName() : SecurityContextHolder.getContext().getAuthentication().getName();
            auditService.log(username, "ROLE_EDIT", "Role", roleId);
            redirectAttributes.addFlashAttribute("success", "Role updated successfully.");
        } catch (NoSuchElementException e) {
            throw new RuntimeException(e.getMessage());
        }
        return "redirect:/roles/" + roleId;
    }

    @PostMapping("/{roleId}/assign-permissions")
    @PreAuthorize("hasPermission(#authentication, 'role.edit')")
    public String assignPermissions(@PathVariable UUID roleId,
                                    @RequestParam(required = false) String[] permissionCodes,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        try {
            String username = (auth != null) ? auth.getName() : SecurityContextHolder.getContext().getAuthentication().getName();
            roleService.assignPermissions(roleId, 
                permissionCodes != null ? java.util.List.of(permissionCodes) : java.util.List.of(),
                username);
            redirectAttributes.addFlashAttribute("success", "Permissions assigned successfully.");
        } catch (NoSuchElementException e) {
            throw new RuntimeException(e.getMessage());
        }
        return "redirect:/roles/" + roleId;
    }
}
