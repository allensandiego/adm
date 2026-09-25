package com.allensandiego.adm.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String loginPost() {
        // Spring Security's UsernamePasswordAuthenticationFilter handles POST /login
        return "redirect:/login?error";
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
