package org.machinesystems.UserMachine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.machinesystems.UserMachine.model.BlacklistedToken;
import org.machinesystems.UserMachine.repository.BlacklistedTokenRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class BlacklistedTokenServiceTest {

    @InjectMocks
    private BlacklistedTokenService blacklistedTokenService;

    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testBlacklistToken() {
        // Given
        String token = "testToken";
        Date expirationDate = new Date();

        // When
        blacklistedTokenService.blacklistToken(token, expirationDate);

        // Then
        ArgumentCaptor<BlacklistedToken> tokenCaptor = ArgumentCaptor.forClass(BlacklistedToken.class);
        verify(blacklistedTokenRepository).save(tokenCaptor.capture());

        BlacklistedToken capturedToken = tokenCaptor.getValue();
        assertEquals(token, capturedToken.getToken());
        assertEquals(expirationDate, capturedToken.getExpirationDate());
    }

    @Test
    void testIsTokenBlacklisted_ReturnsTrue() {
        // Given
        String token = "testToken";
        BlacklistedToken blacklistedToken = new BlacklistedToken(token, new Date());
        when(blacklistedTokenRepository.findByToken(token)).thenReturn(Optional.of(blacklistedToken));

        // When
        boolean result = blacklistedTokenService.isTokenBlacklisted(token);

        // Then
        assertTrue(result);
        verify(blacklistedTokenRepository).findByToken(token);
    }

    @Test
    void testIsTokenBlacklisted_ReturnsFalse() {
        // Given
        String token = "testToken";
        when(blacklistedTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        // When
        boolean result = blacklistedTokenService.isTokenBlacklisted(token);

        // Then
        assertFalse(result);
        verify(blacklistedTokenRepository).findByToken(token);
    }

    @Test
    void testCleanUpExpiredTokens() {
        // Given
        Date now = new Date();
        
        // When
        blacklistedTokenService.cleanUpExpiredTokens();

        // Then
        verify(blacklistedTokenRepository).deleteByExpirationDateBefore(now);
    }
}
