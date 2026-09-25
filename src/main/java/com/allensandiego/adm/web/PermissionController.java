package com.allensandiego.adm.web;

import com.allensandiego.adm.domain.Permission;
import com.allensandiego.adm.dto.PermissionCreateRequest;
import com.allensandiego.adm.service.PermissionService;
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
@RequestMapping("/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    @PreAuthorize("hasPermission(#authentication, 'permission.view')")
    public String listPermissions(@RequestParam(required = false) String search,
                                  @PageableDefault(size = 20) Pageable pageable,
                                  Model model) {
        Page<Permission> permissions = permissionService.listPermissionsAsEntities(pageable, search);
        model.addAttribute("pageTitle", "Permissions");
        model.addAttribute("permissions", permissions);
        model.addAttribute("search", search);
        return "permission/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasPermission(#authentication, 'permission.create')")
    public String newPermissionForm(Model model) {
        model.addAttribute("form", new PermissionCreateRequest());
        return "permission/form";
    }

    @PostMapping
    @PreAuthorize("hasPermission(#authentication, 'permission.create')")
    public String createPermission(@Valid @ModelAttribute PermissionCreateRequest form,
                                   BindingResult result,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "permission/form";
        }
        try {
            String username = (auth != null) ? auth.getName() : SecurityContextHolder.getContext().getAuthentication().getName();
            permissionService.createPermission(form, username);
            redirectAttributes.addFlashAttribute("success", "Permission created successfully.");
        } catch (IllegalArgumentException e) {
            result.rejectValue("code", "error.permission", e.getMessage());
            return "permission/form";
        }
        return "redirect:/permissions";
    }

    @GetMapping("/{permissionId}")
    @PreAuthorize("hasPermission(#authentication, 'permission.view')")
    public String getPermission(@PathVariable UUID permissionId, Model model) {
        Permission permission = permissionService.getPermissionEntity(permissionId);
        model.addAttribute("pageTitle", "Permission Detail");
        model.addAttribute("permission", permission);
        return "permission/detail";
    }

    @PostMapping("/{permissionId}/edit")
    @PreAuthorize("hasPermission(#authentication, 'permission.edit')")
    public String editPermission(@PathVariable UUID permissionId,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            Permission permission = permissionService.getPermissionEntity(permissionId);
            String username = (auth != null) ? auth.getName() : SecurityContextHolder.getContext().getAuthentication().getName();
            permissionService.toggleActive(permissionId, username);
            redirectAttributes.addFlashAttribute("success", "Permission updated successfully.");
        } catch (NoSuchElementException e) {
            throw new RuntimeException(e.getMessage());
        }
        return "redirect:/permissions/" + permissionId;
    }
}
