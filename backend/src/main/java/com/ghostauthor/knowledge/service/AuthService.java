package com.ghostauthor.knowledge.service;

import com.ghostauthor.knowledge.dto.AuthLoginResponse;
import com.ghostauthor.knowledge.dto.AuthSession;
import com.ghostauthor.knowledge.dto.AuthUserResponse;

import java.util.List;

public interface AuthService {
    AuthLoginResponse login(String username, String password, boolean rememberMe);
    AuthSession verifyToken(String token);
    void logout(String token);
    void changePassword(String username, String currentPassword, String newPassword);
    List<AuthUserResponse> listUsers();
    AuthUserResponse upsertUser(String username, String role, String password);
    void deleteUser(String username);
}
