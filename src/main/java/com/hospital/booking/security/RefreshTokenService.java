
package com.hospital.booking.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.hospital.booking.dao.RefreshToken;
import com.hospital.booking.repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final long expirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository repository,
            @Value("${app.jwt.refresh-expiration-ms}")
            long expirationMs) {
        this.repository = repository;
        this.expirationMs = expirationMs;
    }

    @Transactional
    public String createRefreshToken(String username) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        RefreshToken token = new RefreshToken();
        token.setTokenHash(hashToken(rawToken));
        token.setUsername(username);
        token.setExpiryDate(
                Instant.now().plusMillis(expirationMs));
        token.setRevoked(false);

        repository.save(token);

        return rawToken;
    }

    @Transactional
    public String rotateRefreshToken(String rawToken) {
        RefreshToken existing = findValidToken(rawToken);

        // Revoke the old token before issuing a replacement.
        existing.setRevoked(true);
        repository.save(existing);

        return createRefreshToken(existing.getUsername());
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        repository.findByTokenHash(hashToken(rawToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    repository.save(token);
                });
    }

    @Transactional(readOnly = true)
    public String validateRefreshToken(String rawToken) {
        return findValidToken(rawToken).getUsername();
    }

    private RefreshToken findValidToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidToken();
        }

        RefreshToken token = repository
                .findByTokenHash(hashToken(rawToken))
                .orElseThrow(this::invalidToken);

        if (token.isRevoked()
                || !token.getExpiryDate().isAfter(Instant.now())) {
            throw invalidToken();
        }

        return token;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable", exception);
        }
    }

    private ResponseStatusException invalidToken() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Refresh token is invalid, expired, or revoked");
    }
}
