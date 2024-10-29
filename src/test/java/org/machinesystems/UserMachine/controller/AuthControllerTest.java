package org.machinesystems.UserMachine.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.machinesystems.UserMachine.model.RefreshToken;
import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.security.JwtTokenUtil;
import org.machinesystems.UserMachine.service.AuthService;
import org.machinesystems.UserMachine.service.BlacklistedTokenService;
import org.machinesystems.UserMachine.service.RefreshTokenService;
import org.machinesystems.UserMachine.service.UserService;
import org.machinesystems.UserMachine.service.AuditService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.mail.MessagingException;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private BlacklistedTokenService blacklistedTokenService;

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthService authService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthController authController;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize a test user object
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@example.com");
        testUser.setRoles(Set.of("ROLE_USER"));
    }

    @SuppressWarnings("null")
    @Test
    void registerUser_Success() throws Exception {
        Map<String, String> request = Map.of("username", "newuser", "email", "newuser@example.com", "password", "password");

        when(userService.registerUser(anyString(), anyString(), anyString(), anySet())).thenReturn(testUser);

        ResponseEntity<?> response = authController.registerUser(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("User registered successfully", ((Map<?, ?>) response.getBody()).get("message"));
        verify(auditService).logAuditEvent(eq("INFO"), eq("AuthController"), contains("User registered"), any(), isNull());
    }

    @SuppressWarnings("null")
    @Test
    void registerUser_Failure() throws Exception {
        Map<String, String> request = Map.of("username", "newuser", "email", "newuser@example.com", "password", "password");

        when(userService.registerUser(anyString(), anyString(), anyString(), anySet())).thenThrow(new RuntimeException("Registration failed"));

        ResponseEntity<?> response = authController.registerUser(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("User registration failed", ((Map<?, ?>) response.getBody()).get("message"));
        verify(auditService).logAuditEvent(eq("ERROR"), eq("AuthController"), contains("User registration failed"), any(), any());
    }

    @SuppressWarnings("null")
    @Test
    void verifyUser_Success() {
        when(authService.verify(anyString())).thenReturn(true);

        ResponseEntity<?> response = authController.verifyUser("valid_code");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("User verified", ((Map<?, ?>) response.getBody()).get("message"));
        verify(auditService).logAuditEvent(eq("INFO"), eq("AuthController"), contains("User verified"), any(), isNull());
    }

    @SuppressWarnings("null")
    @Test
    void verifyUser_Failure() {
        when(authService.verify(anyString())).thenReturn(false);

        ResponseEntity<?> response = authController.verifyUser("invalid_code");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User not verified", ((Map<?, ?>) response.getBody()).get("message"));
        verify(auditService).logAuditEvent(eq("ERROR"), eq("AuthController"), contains("User verification failed"), any(), isNull());
    }

    @SuppressWarnings("null")
    @Test
    void loginUser_Success() {
        Map<String, String> request = Map.of("username", "testuser", "password", "password");

        when(authService.loginUser(anyString(), anyString())).thenReturn("mockAccessToken");
        when(userService.getUserByUsername(anyString())).thenReturn(testUser);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(new RefreshToken("sampleToken", testUser, Instant.now().plus(Duration.ofDays(1))));

        ResponseEntity<?> response = authController.loginUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("mockAccessToken", ((Map<?, ?>) response.getBody()).get("accessToken"));
        verify(auditService).logAuditEvent(eq("INFO"), eq("AuthController"), contains("User logged in"), any(), isNull());
    }

    @SuppressWarnings("null")
    @Test
    void loginUser_Failure() {
        Map<String, String> request = Map.of("username", "testuser", "password", "wrong_password");

        when(authService.loginUser(anyString(), anyString())).thenThrow(new IllegalArgumentException("Invalid password"));

        ResponseEntity<?> response = authController.loginUser(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid password", ((Map<?, ?>) response.getBody()).get("message"));
        verify(auditService).logAuditEvent(eq("ERROR"), eq("AuthController"), contains("Login failed"), any(), anyString());
    }

    @SuppressWarnings("null")
    @Test
    void refreshToken_Success() {
        Map<String, String> request = Map.of("refreshToken", "validRefreshToken", "accessToken", "expiredAccessToken");

        when(refreshTokenService.findByToken(anyString())).thenReturn(Optional.of(new RefreshToken("sampleToken", testUser, Instant.now().plus(Duration.ofDays(1)))));
        when(jwtTokenUtil.getExpirationDateFromToken(anyString())).thenReturn(new Date());
        when(jwtTokenUtil.generateAccessToken(anyString(), anySet())).thenReturn("newAccessToken");

        ResponseEntity<?> response = authController.refreshToken(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("newAccessToken", ((Map<?, ?>) response.getBody()).get("accessToken"));
        verify(auditService).logAuditEvent(eq("INFO"), eq("AuthController"), contains("Token refreshed"), any(), isNull());
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Test
void refreshToken_Failure() {
    String invalidRefreshToken = "invalidRefreshToken";
    
    // Mock the refreshTokenService to return an empty Optional if "invalidRefreshToken" is passed
    when(refreshTokenService.findByToken(eq(invalidRefreshToken))).thenReturn(Optional.empty());

    Map<String, String> request = Map.of("refreshToken", invalidRefreshToken);
    ResponseEntity<?> response = authController.refreshToken(request);

    // Check that the response status is UNAUTHORIZED (401)
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

    // Check that the response message matches the expected error
    assertEquals("Token refresh failed: Invalid refresh token", ((Map<String, String>) response.getBody()).get("message"));
}

    @SuppressWarnings("null")
    @Test
    void requestPasswordReset_Success() throws UnsupportedEncodingException, MessagingException {
        Map<String, String> request = Map.of("email", "testuser@example.com");

        doNothing().when(authService).resetPassword(anyString(), anyString());

        ResponseEntity<?> response = authController.requestPasswordReset(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password reset email sent", ((Map<?, ?>) response.getBody()).get("message"));
        verify(auditService).logAuditEvent(eq("INFO"), eq("AuthController"), contains("Password reset requested"), any(), isNull());
    }

    @SuppressWarnings("null")
    @Test
    void requestPasswordReset_Failure() throws UnsupportedEncodingException, MessagingException {
        Map<String, String> request = Map.of("email", "nonexistent@example.com");

        doThrow(new RuntimeException("Error sending email")).when(authService).resetPassword(anyString(), anyString());

        ResponseEntity<?> response = authController.requestPasswordReset(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error sending password reset email", ((Map<?, ?>) response.getBody()).get("message"));
        verify(auditService).logAuditEvent(eq("ERROR"), eq("AuthController"), contains("Password reset request failed"), any(), anyString());
    }

    @SuppressWarnings("null")
    @Test
    void resetPassword_Success() {
        when(authService.verifyPasswordReset(anyString(), anyString())).thenReturn(true);

        ResponseEntity<?> response = authController.resetPassword("validToken", "newPassword");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password reset successfully", ((Map<?, ?>) response.getBody()).get("message"));
        verify(auditService).logAuditEvent(eq("INFO"), eq("AuthController"), contains("Password reset for token"), any(), isNull());
    }

    @SuppressWarnings("null")
    @Test
    void resetPassword_Failure() {
        when(authService.verifyPasswordReset(anyString(), anyString())).thenReturn(false);

        ResponseEntity<?> response = authController.resetPassword("invalidToken", "newPassword");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid or expired token", ((Map<?, ?>) response.getBody()).get("message"));
        verify(auditService).logAuditEvent(eq("ERROR"), eq("AuthController"), contains("Password reset failed"), any(), isNull());
    }
}
