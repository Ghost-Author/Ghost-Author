package com.ghostauthor.knowledge.service.impl;

import com.ghostauthor.knowledge.dto.AuthLoginResponse;
import com.ghostauthor.knowledge.dto.AuthSession;
import com.ghostauthor.knowledge.service.AuthService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String BCRYPT_PREFIX = "{bcrypt}";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EDITOR = "EDITOR";
    private static final String ROLE_VIEWER = "VIEWER";

    @Value("${knowledge.auth.users:admin:admin123}")
    private String usersConfig;

    @Value("${knowledge.auth.token-ttl-hours:168}")
    private long tokenTtlHours;

    @Value("${knowledge.auth.remember-token-ttl-hours:720}")
    private long rememberTokenTtlHours;

    @Value("${knowledge.auth.max-failures:8}")
    private int maxFailures;

    @Value("${knowledge.auth.lock-minutes:10}")
    private long lockMinutes;

    private final Map<String, UserCredential> users = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, FailedAttempt> failedAttempts = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
            String rest = trimmed.substring(splitAt + 1).trim();
            if (username.isEmpty() || rest.isEmpty()) {
                continue;
            }
            String password = rest;
            String role = ROLE_ADMIN;
            int roleSplit = rest.lastIndexOf(':');
            if (roleSplit > 0 && roleSplit < rest.length() - 1) {
                String maybeRole = normalizeRole(rest.substring(roleSplit + 1).trim());
                if (maybeRole != null) {
                    password = rest.substring(0, roleSplit).trim();
                    role = maybeRole;
                }
            }
            if (!password.isEmpty()) {
                users.put(username, new UserCredential(password, role));
            }
        }
        if (users.isEmpty()) {
            users.put("admin", new UserCredential("admin123", ROLE_ADMIN));
        }
    }

    @Override
    public AuthLoginResponse login(String username, String password, boolean rememberMe) {
        String cleanUser = username == null ? "" : username.trim();
        String cleanPass = password == null ? "" : password;
        ensureLoginNotLocked(cleanUser);
        UserCredential credential = users.get(cleanUser);
        if (credential == null || !passwordMatches(cleanPass, credential.password)) {
            markLoginFailure(cleanUser);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        failedAttempts.remove(cleanUser);

        cleanupExpiredSessions();
        String token = UUID.randomUUID().toString().replace("-", "");
        long ttlHours = rememberMe ? Math.max(1, rememberTokenTtlHours) : Math.max(1, tokenTtlHours);
        long expiresAt = Instant.now().plusSeconds(ttlHours * 3600).toEpochMilli();
        sessions.put(token, new Session(cleanUser, credential.role, expiresAt));
        return new AuthLoginResponse(token, cleanUser, credential.role, expiresAt);
    }

    @Override
    public AuthSession verifyToken(String token) {
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
        return new AuthSession(session.username, session.role);
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

    private void ensureLoginNotLocked(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        FailedAttempt attempt = failedAttempts.get(username);
        if (attempt == null || attempt.lockedUntil <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (attempt.lockedUntil > now) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "登录失败次数过多，请稍后再试");
        }
        failedAttempts.remove(username);
    }

    private void markLoginFailure(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        int limit = Math.max(2, maxFailures);
        long lockMillis = Math.max(1, lockMinutes) * 60_000L;
        failedAttempts.compute(username, (key, old) -> {
            long now = System.currentTimeMillis();
            FailedAttempt next = old == null ? new FailedAttempt() : old;
            if (next.lockedUntil > 0 && next.lockedUntil <= now) {
                next.count = 0;
                next.lockedUntil = 0;
            }
            next.count += 1;
            if (next.count >= limit) {
                next.count = 0;
                next.lockedUntil = now + lockMillis;
            }
            return next;
        });
    }

    private boolean passwordMatches(String rawPassword, String configuredPassword) {
        String configured = configuredPassword == null ? "" : configuredPassword.trim();
        if (configured.isEmpty()) {
            return false;
        }
        if (configured.startsWith(BCRYPT_PREFIX)) {
            String hash = configured.substring(BCRYPT_PREFIX.length()).trim();
            return !hash.isEmpty() && passwordEncoder.matches(rawPassword, hash);
        }
        if (configured.startsWith("$2a$") || configured.startsWith("$2b$") || configured.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, configured);
        }
        return configured.equals(rawPassword);
    }

    private String normalizeRole(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String role = raw.trim().toUpperCase();
        if (ROLE_ADMIN.equals(role) || ROLE_EDITOR.equals(role) || ROLE_VIEWER.equals(role)) {
            return role;
        }
        return null;
    }

    private record Session(String username, String role, long expiresAt) {
    }

    private record UserCredential(String password, String role) {
    }

    private static class FailedAttempt {
        private int count;
        private long lockedUntil;
    }
}
