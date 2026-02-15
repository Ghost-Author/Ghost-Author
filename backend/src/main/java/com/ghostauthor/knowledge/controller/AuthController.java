package com.ghostauthor.knowledge.controller;

import com.ghostauthor.knowledge.dto.AuthLoginRequest;
import com.ghostauthor.knowledge.dto.AuthLoginResponse;
import com.ghostauthor.knowledge.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthLoginResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @GetMapping("/me")
    public Map<String, String> me(HttpServletRequest request) {
        Object username = request.getAttribute("authUser");
        return Map.of("username", username == null ? "" : String.valueOf(username));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return;
        }
        authService.logout(header.substring(7));
    }
}
