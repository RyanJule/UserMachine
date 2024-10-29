package org.machinesystems.UserMachine.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.security.CustomUserDetails;
import org.machinesystems.UserMachine.security.JwtTokenUtil;
import org.machinesystems.UserMachine.service.AuditService;
import org.machinesystems.UserMachine.service.BlacklistedTokenService;
import org.machinesystems.UserMachine.service.RefreshTokenService;
import org.machinesystems.UserMachine.service.UserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private BlacklistedTokenService blacklistedTokenService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserController userController;

    private UserDetails userDetails;
    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup test user details
        user = new User();
        user.setUsername("john");
        user.setEmail("john@example.com");
        user.setRoles(Set.of("ROLE_USER"));

        userDetails = new CustomUserDetails(user);
    }

    @Test
    void testUpdateProfile_Success() {
        String authorizationHeader = "Bearer oldAccessToken";
        Map<String, String> request = Map.of("username", "newUsername", "email", "newEmail@example.com");

        UserDetails userDetails = mock(UserDetails.class);

        // Mock user details and dependencies
        when(userDetails.getUsername()).thenReturn("currentUsername"); // Current username
        when(userService.getUserByUsername("currentUsername")).thenReturn(user); // Existing user lookup
        when(userService.updateUserProfile(anyString(), anyString(), anyString())).thenReturn(user); // Update operation
        when(jwtTokenUtil.getExpirationDateFromToken(anyString())).thenReturn(new Date()); // Expiration for old token
        when(jwtTokenUtil.generateAccessToken(anyString(), anySet())).thenReturn("newAccessToken"); // New token generation

        try {
            // Execute the controller method
            ResponseEntity<?> response = userController.updateProfile(userDetails, authorizationHeader, request);

            // Assertions
            assertEquals(HttpStatus.OK, response.getStatusCode());

            // Verifications
            verify(userService).updateUserProfile("currentUsername", "newUsername", "newEmail@example.com");
            verify(blacklistedTokenService).blacklistToken(eq("oldAccessToken"), any(Date.class));
            verify(auditService).logAuditEvent("INFO", "UserController", "Profile updated", Thread.currentThread().getName(), null);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception occurred: " + e.getMessage());
        }
    }

    @Test
    void testChangePassword_Success() {
        Map<String, String> request = new HashMap<>();
        request.put("currentPassword", "oldPassword");
        request.put("newPassword", "newPassword");

        ResponseEntity<?> response = userController.changePassword(userDetails, request);

        // Verify the correct result and that services were called
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password updated successfully", response.getBody());
        verify(userService).changePassword(userDetails.getUsername(), "oldPassword", "newPassword");
        verify(auditService).logAuditEvent("INFO", "UserController", "Password changed", Thread.currentThread().getName(), null);
    }

    @Test
    void testDeleteUser_Success() {
        String authorizationHeader = "Bearer accessToken";

        when(userService.getUserByUsername(anyString())).thenReturn(user);
        when(jwtTokenUtil.getExpirationDateFromToken(anyString())).thenReturn(new Date());

        ResponseEntity<?> response = userController.deleteUser(userDetails, authorizationHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(refreshTokenService).deleteByUser(user);

        // Use `eq` for the token and `any(Date.class)` for the Date argument to ignore the exact timestamp
        verify(blacklistedTokenService).blacklistToken(eq("accessToken"), any(Date.class));
        verify(userService).deleteUserByUsername(user.getUsername());
        verify(auditService).logAuditEvent("INFO", "UserController", "User deleted", Thread.currentThread().getName(), null);
    }

    @SuppressWarnings({ "null", "rawtypes" })
    @Test
    void testUpdateProfile_ProfileUpdateFails() {
        String authorizationHeader = "Bearer oldAccessToken";
        Map<String, String> request = new HashMap<>();
        request.put("username", "newJohn");
        request.put("email", "newJohn@example.com");

        when(userService.updateUserProfile(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("Profile update failed"));

        ResponseEntity<?> response = userController.updateProfile(userDetails, authorizationHeader, request);

        // Verify failure response and audit logging
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Profile update failed", ((Map) response.getBody()).get("message"));
        verify(auditService).logAuditEvent("ERROR", "UserController", "Profile update failed", Thread.currentThread().getName(), "Profile update failed");
    }

    @SuppressWarnings({ "null", "rawtypes" })
    @Test
    void testDeleteUser_DeletionFails() {
        String authorizationHeader = "Bearer accessToken";
        
        when(userService.getUserByUsername(anyString())).thenThrow(new RuntimeException("Failed to delete user"));

        ResponseEntity<?> response = userController.deleteUser(userDetails, authorizationHeader);

        // Verify failure response and audit logging
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Failed to delete user", ((Map) response.getBody()).get("message"));
        verify(auditService).logAuditEvent("ERROR", "UserController", "User deletion failed", Thread.currentThread().getName(), "Failed to delete user");
    }
}
