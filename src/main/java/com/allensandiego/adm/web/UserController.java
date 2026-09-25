package com.allensandiego.adm.web;

import com.allensandiego.adm.domain.User;
import com.allensandiego.adm.dto.UserCreateRequest;
import com.allensandiego.adm.service.RoleService;
import com.allensandiego.adm.service.UserService;
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
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final RoleService roleService;

    public UserController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasPermission(#authentication, 'user.view')")
    public String listUsers(@RequestParam(required = false) String search,
                            @PageableDefault(size = 20) Pageable pageable,
                            Model model) {
        Page<User> users = userService.listUsersAsEntities(pageable, search);
        model.addAttribute("pageTitle", "Users");
        model.addAttribute("users", users);
        model.addAttribute("search", search);
        return "user/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasPermission(#authentication, 'user.create')")
    public String newUserForm(Model model) {
        model.addAttribute("form", new UserCreateRequest());
        return "user/form";
    }

    @PostMapping
    @PreAuthorize("hasPermission(#authentication, 'user.create')")
    public String createUser(@Valid @ModelAttribute UserCreateRequest form,
                             @RequestParam(required = false) String role,
                             BindingResult result,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "user/form";
        }
        try {
            String username = (auth != null) ? auth.getName() : SecurityContextHolder.getContext().getAuthentication().getName();
            userService.createUser(form, username);
            if (role != null && !role.isEmpty()) {
                userService.assignRolesByCode(form.getUsername(), java.util.List.of(role), username);
            }
            redirectAttributes.addFlashAttribute("success", "User created successfully.");
        } catch (IllegalArgumentException e) {
            result.rejectValue("username", "error.user", e.getMessage());
            return "user/form";
        }
        return "redirect:/users";
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasPermission(#authentication, 'user.view')")
    public String getUser(@PathVariable UUID userId, Model model) {
        User user = userService.getUserEntity(userId);
        model.addAttribute("pageTitle", "User Detail");
        model.addAttribute("user", user);
        model.addAttribute("roles", roleService.listRolesAsEntities(org.springframework.data.domain.Pageable.unpaged(), null));
        return "user/detail";
    }

    @PostMapping("/{userId}/edit")
    @PreAuthorize("hasPermission(#authentication, 'user.edit')")
    public String editUser(@PathVariable UUID userId,
                           @RequestParam(required = false) boolean active,
                           Authentication auth,
                           RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserEntity(userId);
            user.setStatus(active ? User.UserStatus.ACTIVE : User.UserStatus.INACTIVE);
            String username = (auth != null) ? auth.getName() : SecurityContextHolder.getContext().getAuthentication().getName();
            userService.updateProfile(userId, user.getUsername(), username);
            redirectAttributes.addFlashAttribute("success", "User updated successfully.");
        } catch (NoSuchElementException e) {
            throw new RuntimeException(e.getMessage());
        }
        return "redirect:/users/" + userId;
    }

    @PostMapping("/{userId}/assign-roles")
    @PreAuthorize("hasPermission(#authentication, 'user.roles.assign')")
    public String assignRoles(@PathVariable UUID userId,
                              @RequestParam(required = false) String[] roleCodes,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        try {
            String username = (auth != null) ? auth.getName() : SecurityContextHolder.getContext().getAuthentication().getName();
            userService.assignRoles(userId,
                roleCodes != null ? java.util.List.of(roleCodes) : java.util.List.of(),
                username);
            redirectAttributes.addFlashAttribute("success", "Roles assigned successfully.");
        } catch (NoSuchElementException e) {
            throw new RuntimeException(e.getMessage());
        }
        return "redirect:/users/" + userId;
    }

    @PostMapping("/delete")
    @PreAuthorize("hasPermission(#authentication, 'user.delete')")
    public String deleteUser(@RequestParam UUID userId,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        try {
            String username = (auth != null) ? auth.getName() : SecurityContextHolder.getContext().getAuthentication().getName();
            userService.deleteUser(userId, username);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully.");
        } catch (NoSuchElementException e) {
            throw new RuntimeException(e.getMessage());
        }
        return "redirect:/users";
    }

    @PostMapping("/assign-role")
    @PreAuthorize("hasPermission(#authentication, 'user.roles.assign')")
    public String assignRole(@RequestParam UUID userId,
                             @RequestParam String role,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        try {
            String username = (auth != null) ? auth.getName() : SecurityContextHolder.getContext().getAuthentication().getName();
            userService.assignRolesByCode(userId.toString(), java.util.List.of(role), username);
            redirectAttributes.addFlashAttribute("success", "Role assigned successfully.");
        } catch (NoSuchElementException e) {
            throw new RuntimeException(e.getMessage());
        }
        return "redirect:/users";
    }
}
