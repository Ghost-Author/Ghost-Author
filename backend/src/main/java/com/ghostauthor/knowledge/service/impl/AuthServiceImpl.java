package com.ghostauthor.knowledge.service.impl;

import com.ghostauthor.knowledge.dto.AuthLoginResponse;
import com.ghostauthor.knowledge.service.AuthService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthServiceImpl implements AuthService {

    @Value("${knowledge.auth.users:admin:admin123}")
    private String usersConfig;

    @Value("${knowledge.auth.token-ttl-hours:168}")
    private long tokenTtlHours;

    private final Map<String, String> users = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void initUsers() {
        users.clear();
        String[] pairs = usersConfig.split(",");
        for (String pair : pairs) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int splitAt = trimmed.indexOf(':');
            if (splitAt <= 0 || splitAt >= trimmed.length() - 1) {
                continue;
            }
            String username = trimmed.substring(0, splitAt).trim();
            String password = trimmed.substring(splitAt + 1).trim();
            if (!username.isEmpty() && !password.isEmpty()) {
                users.put(username, password);
            }
        }
        if (users.isEmpty()) {
            users.put("admin", "admin123");
        }
    }

    @Override
    public AuthLoginResponse login(String username, String password) {
        String cleanUser = username == null ? "" : username.trim();
        String cleanPass = password == null ? "" : password;
        String expected = users.get(cleanUser);
        if (expected == null || !expected.equals(cleanPass)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        cleanupExpiredSessions();
        String token = UUID.randomUUID().toString().replace("-", "");
        long expiresAt = Instant.now().plusSeconds(Math.max(1, tokenTtlHours) * 3600).toEpochMilli();
        sessions.put(token, new Session(cleanUser, expiresAt));
        return new AuthLoginResponse(token, cleanUser, expiresAt);
    }

    @Override
    public String verifyToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Session session = sessions.get(token.trim());
        if (session == null) {
            return null;
        }
        if (session.expiresAt < System.currentTimeMillis()) {
            sessions.remove(token);
            return null;
        }
        return session.username;
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        sessions.remove(token.trim());
    }

    private void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf((entry) -> entry.getValue().expiresAt < now);
    }

    private record Session(String username, long expiresAt) {
    }
}
