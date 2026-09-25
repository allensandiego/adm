package com.allensandiego.adm.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ControllerAdvice;

import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAuthorizationDenied(AuthorizationDeniedException ex, Model model, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = auth != null && ("anonymousUser".equals(auth.getPrincipal()) || "anonymous".equals(auth.getPrincipal()));
        System.out.println("=== GlobalExceptionHandler.handleAuthorizationDenied ===");
        System.out.println("Auth: " + auth);
        System.out.println("Is Anonymous: " + isAnonymous);
        if (isAnonymous || !auth.isAuthenticated()) {
            model.addAttribute("error", "Authentication required");
            return "error/401";
        }
        model.addAttribute("error", "Access denied");
        return "error/403";
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<String> handleAuthCredentialsNotFound(AuthenticationCredentialsNotFoundException ex, Model model, HttpServletRequest request) {
        System.out.println("=== GlobalExceptionHandler.handleAuthCredentialsNotFound ===");
        ex.printStackTrace();
        model.addAttribute("error", "Authentication required");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("error/401");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = auth != null && ("anonymousUser".equals(auth.getPrincipal()) || "anonymous".equals(auth.getPrincipal()));
        System.out.println("=== GlobalExceptionHandler.handleAccessDenied ===");
        System.out.println("Auth: " + auth);
        System.out.println("Is Anonymous: " + isAnonymous);
        if (isAnonymous || !auth.isAuthenticated()) {
            model.addAttribute("error", "Authentication required");
            return "error/401";
        }
        model.addAttribute("error", "Access denied");
        return "error/403";
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoSuchElementException ex, Model model, HttpServletRequest request) {
        model.addAttribute("error", "Resource not found");
        return "error/404";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleConflict(IllegalArgumentException ex, Model model, HttpServletRequest request) {
        System.err.println("=== IllegalArgumentException caught ===");
        ex.printStackTrace();
        model.addAttribute("error", ex.getMessage());
        return "error/409";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResourceFound(NoResourceFoundException ex, Model model, HttpServletRequest request) {
        model.addAttribute("error", "Resource not found");
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception ex, Model model, HttpServletRequest request) {
        System.err.println("=== General Exception caught ===");
        ex.printStackTrace();
        model.addAttribute("error", "An unexpected error occurred");
        return "error/500";
    }
}
