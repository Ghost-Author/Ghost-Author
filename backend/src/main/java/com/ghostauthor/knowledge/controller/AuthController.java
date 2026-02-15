package com.ghostauthor.knowledge.controller;

import com.ghostauthor.knowledge.dto.AuthLoginRequest;
import com.ghostauthor.knowledge.dto.AuthLoginResponse;
import com.ghostauthor.knowledge.dto.AuthUserResponse;
import com.ghostauthor.knowledge.dto.AuthUserUpsertRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ROLE_ADMIN = "ADMIN";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthLoginResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request.username(), request.password(), Boolean.TRUE.equals(request.rememberMe()));
    }

    @GetMapping("/me")
    public Map<String, String> me(HttpServletRequest request) {
        Object username = request.getAttribute("authUser");
        Object role = request.getAttribute("authRole");
        return Map.of(
                "username", username == null ? "" : String.valueOf(username),
                "role", role == null ? "ADMIN" : String.valueOf(role)
        );
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

    @GetMapping("/users")
    public List<AuthUserResponse> listUsers(HttpServletRequest request) {
        requireAdmin(request);
        return authService.listUsers();
    }

    @PostMapping("/users")
    public AuthUserResponse upsertUser(@Valid @RequestBody AuthUserUpsertRequest request, HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return authService.upsertUser(request.username(), request.role(), request.password());
    }

    @DeleteMapping("/users/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable String username, HttpServletRequest request) {
        requireAdmin(request);
        authService.deleteUser(username);
    }

    private void requireAdmin(HttpServletRequest request) {
        Object roleAttr = request.getAttribute("authRole");
        String role = roleAttr == null ? "" : String.valueOf(roleAttr).trim().toUpperCase();
        if (ROLE_ADMIN.equals(role)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅管理员可管理用户");
    }
}
