package com.ghostauthor.knowledge.service;

import com.ghostauthor.knowledge.dto.AuthLoginResponse;

public interface AuthService {
    AuthLoginResponse login(String username, String password, boolean rememberMe);
    String verifyToken(String token);
    void logout(String token);
}
