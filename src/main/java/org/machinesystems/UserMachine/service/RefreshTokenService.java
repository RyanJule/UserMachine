package org.machinesystems.UserMachine.service;

import org.machinesystems.UserMachine.model.RefreshToken;
import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private final long refreshTokenDurationMs = 86400000L; // 1 day

    // Generate a new refresh token for a user
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString()); // Generate a unique token
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));

        return refreshTokenRepository.save(refreshToken);
    }

    // Find refresh token by token string
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    // Verify if the refresh token is valid
    public void verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token has expired");
        }
    }

    // Delete refresh token by user (used on logout)
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
