package com.ghostauthor.knowledge.service;

import com.ghostauthor.knowledge.dto.AuthLoginResponse;
import com.ghostauthor.knowledge.dto.AuthSession;

public interface AuthService {
    AuthLoginResponse login(String username, String password, boolean rememberMe);
    AuthSession verifyToken(String token);
    void logout(String token);
}
