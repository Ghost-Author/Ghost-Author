package com.ghostauthor.knowledge.service;

import com.ghostauthor.knowledge.dto.AuthLoginResponse;

public interface AuthService {
    AuthLoginResponse login(String username, String password);
    String verifyToken(String token);
}
