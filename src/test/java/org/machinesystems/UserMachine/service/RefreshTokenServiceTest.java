package org.machinesystems.UserMachine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.machinesystems.UserMachine.model.RefreshToken;
import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.repository.RefreshTokenRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize a test user
        user = new User();
        user.setUsername("john");

        // Initialize a test refresh token
        refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(86400000L)); // Set expiry to 24 hours
    }

    @Test
    void testCreateRefreshToken() {
        // Mock the repository save behavior
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Call the service to create a refresh token
        RefreshToken createdToken = refreshTokenService.createRefreshToken(user);

        // Verify the token was saved
        verify(refreshTokenRepository).save(any(RefreshToken.class));

        // Assertions
        assertNotNull(createdToken.getToken());
        assertEquals(user, createdToken.getUser());
        assertTrue(createdToken.getExpiryDate().isAfter(Instant.now()));
    }

    @Test
    void testFindByToken_Success() {
        // Mock the repository to return the refresh token when queried
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(refreshToken));

        // Call the service
        Optional<RefreshToken> foundToken = refreshTokenService.findByToken(refreshToken.getToken());

        // Verify the result
        assertTrue(foundToken.isPresent());
        assertEquals(refreshToken.getToken(), foundToken.get().getToken());

        // Verify the repository call
        verify(refreshTokenRepository).findByToken(refreshToken.getToken());
    }

    @Test
    void testFindByToken_Failure() {
        // Mock repository to return empty if the token is not found
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

        // Call the service
        Optional<RefreshToken> foundToken = refreshTokenService.findByToken("invalidToken");

        // Assertions
        assertFalse(foundToken.isPresent());

        // Verify the repository call
        verify(refreshTokenRepository).findByToken("invalidToken");
    }

    @Test
    void testVerifyExpiration_NotExpired() {
        // Call the service to verify expiration
        assertDoesNotThrow(() -> refreshTokenService.verifyExpiration(refreshToken));
    }

    @Test
    void testVerifyExpiration_Expired() {
        // Simulate an expired refresh token
        refreshToken.setExpiryDate(Instant.now().minusMillis(1000)); // Set it to expire 1 second ago

        // Call the service and expect an exception
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            refreshTokenService.verifyExpiration(refreshToken);
        });

        // Assertions
        assertEquals("Refresh token has expired", thrown.getMessage());

        // Verify that the token is deleted after expiration
        verify(refreshTokenRepository).delete(refreshToken);
    }

    @Test
    void testDeleteByUser() {
        // Call the service to delete by user
        refreshTokenService.deleteByUser(user);

        // Verify the repository call
        verify(refreshTokenRepository).deleteByUser(user);
    }
}
