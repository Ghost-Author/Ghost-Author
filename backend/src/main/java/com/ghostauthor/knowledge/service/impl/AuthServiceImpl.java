package com.ghostauthor.knowledge.service.impl;

import com.ghostauthor.knowledge.dto.AuthLoginResponse;
import com.ghostauthor.knowledge.dto.AuthSession;
import com.ghostauthor.knowledge.dto.AuthUserResponse;
import com.ghostauthor.knowledge.entity.AuthUserEntity;
import com.ghostauthor.knowledge.repository.AuthUserRepository;
import com.ghostauthor.knowledge.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
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

    private final AuthUserRepository authUserRepository;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, FailedAttempt> failedAttempts = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private volatile boolean usersBootstrapped = false;

    public AuthServiceImpl(AuthUserRepository authUserRepository) {
        this.authUserRepository = authUserRepository;
    }

    @Override
    public AuthLoginResponse login(String username, String password, boolean rememberMe) {
        bootstrapUsersIfNeeded();

        String cleanUser = username == null ? "" : username.trim();
        String cleanPass = password == null ? "" : password;
        ensureLoginNotLocked(cleanUser);

        AuthUserEntity user = authUserRepository.findByUsername(cleanUser).orElse(null);
        if (user == null || !passwordMatches(cleanPass, user.getPassword())) {
            markLoginFailure(cleanUser);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        failedAttempts.remove(cleanUser);

        cleanupExpiredSessions();
        String token = UUID.randomUUID().toString().replace("-", "");
        long ttlHours = rememberMe ? Math.max(1, rememberTokenTtlHours) : Math.max(1, tokenTtlHours);
        long expiresAt = Instant.now().plusSeconds(ttlHours * 3600).toEpochMilli();
        sessions.put(token, new Session(cleanUser, user.getRole(), expiresAt));
        return new AuthLoginResponse(token, cleanUser, user.getRole(), expiresAt);
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

    @Override
    public List<AuthUserResponse> listUsers() {
        bootstrapUsersIfNeeded();
        return authUserRepository.findAll().stream()
                .map((entity) -> new AuthUserResponse(entity.getUsername(), entity.getRole()))
                .sorted(Comparator.comparing(AuthUserResponse::username))
                .toList();
    }

    @Override
    public AuthUserResponse upsertUser(String username, String role, String password) {
        bootstrapUsersIfNeeded();

        String cleanUser = username == null ? "" : username.trim();
        if (cleanUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名不能为空");
        }
        String cleanRole = normalizeRole(role);
        if (cleanRole == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色非法");
        }

        AuthUserEntity existing = authUserRepository.findByUsername(cleanUser).orElse(null);
        if (existing != null
                && ROLE_ADMIN.equals(existing.getRole())
                && !ROLE_ADMIN.equals(cleanRole)
                && authUserRepository.countByRole(ROLE_ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少保留一个 ADMIN 用户");
        }

        String providedPassword = password == null ? "" : password.trim();
        if (existing == null && providedPassword.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新建用户必须提供密码");
        }

        AuthUserEntity target = existing == null ? new AuthUserEntity() : existing;
        target.setUsername(cleanUser);
        target.setRole(cleanRole);
        if (!providedPassword.isEmpty()) {
            target.setPassword(providedPassword);
        }
        authUserRepository.save(target);
        return new AuthUserResponse(cleanUser, cleanRole);
    }

    @Override
    public void deleteUser(String username) {
        bootstrapUsersIfNeeded();

        String cleanUser = username == null ? "" : username.trim();
        if (cleanUser.isEmpty()) {
            return;
        }
        AuthUserEntity user = authUserRepository.findByUsername(cleanUser).orElse(null);
        if (user == null) {
            return;
        }
        if (ROLE_ADMIN.equals(user.getRole()) && authUserRepository.countByRole(ROLE_ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少保留一个 ADMIN 用户");
        }
        authUserRepository.delete(user);
        sessions.entrySet().removeIf((entry) -> cleanUser.equals(entry.getValue().username));
        failedAttempts.remove(cleanUser);
    }

    private synchronized void bootstrapUsersIfNeeded() {
        if (usersBootstrapped) {
            return;
        }
        if (authUserRepository.count() == 0) {
            List<UserCredential> configuredUsers = parseUsersConfig(usersConfig);
            if (configuredUsers.isEmpty()) {
                configuredUsers = List.of(new UserCredential("admin", "admin123", ROLE_ADMIN));
            }
            configuredUsers.forEach((item) -> {
                AuthUserEntity entity = new AuthUserEntity();
                entity.setUsername(item.username);
                entity.setPassword(item.password);
                entity.setRole(item.role);
                authUserRepository.save(entity);
            });
        }
        if (authUserRepository.countByRole(ROLE_ADMIN) == 0) {
            AuthUserEntity first = authUserRepository.findAll().stream().findFirst().orElse(null);
            if (first == null) {
                first = new AuthUserEntity();
                first.setUsername("admin");
                first.setPassword("admin123");
            }
            first.setRole(ROLE_ADMIN);
            authUserRepository.save(first);
        }
        usersBootstrapped = true;
    }

    private List<UserCredential> parseUsersConfig(String rawConfig) {
        if (rawConfig == null || rawConfig.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(rawConfig.split(","))
                .map(String::trim)
                .filter((item) -> !item.isEmpty())
                .map(this::parseSingleUser)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private UserCredential parseSingleUser(String item) {
        int splitAt = item.indexOf(':');
        if (splitAt <= 0 || splitAt >= item.length() - 1) {
            return null;
        }
        String username = item.substring(0, splitAt).trim();
        String rest = item.substring(splitAt + 1).trim();
        if (username.isEmpty() || rest.isEmpty()) {
            return null;
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
        if (password.isEmpty()) {
            return null;
        }
        return new UserCredential(username, password, role);
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

    private record UserCredential(String username, String password, String role) {
    }

    private static class FailedAttempt {
        private int count;
        private long lockedUntil;
    }
}
