package org.machinesystems.UserMachine.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.service.AuthService;
import org.machinesystems.UserMachine.service.RefreshTokenService;
import org.machinesystems.UserMachine.service.UserService;
import org.machinesystems.UserMachine.service.AuditService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


class AdminControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminController adminController;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@example.com");
        testUser.setRoles(Set.of("ROLE_USER"));
    }

    @SuppressWarnings({ "null", "rawtypes" })
    @Test
    void registerAdminUser_Success() throws Exception {
        Map<String, String> request = Map.of(
                "username", "newuser",
                "email", "newuser@example.com",
                "password", "password",
                "role", "ROLE_USER"
        );

        when(userService.registerUser(anyString(), anyString(), anyString(), anySet()))
                .thenReturn(testUser);

        ResponseEntity<?> response = adminController.registerAdminUser(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("User registered successfully", ((Map) response.getBody()).get("message"));
        verify(auditService).logAuditEvent(eq("INFO"), eq("AdminController"), contains("Admin registered new user"), any(), isNull());
    }
    @SuppressWarnings({ "null", "rawtypes" })
    @Test
    void registerAdminUser_Failure() throws Exception {
        Map<String, String> request = Map.of(
                "username", "newuser",
                "email", "newuser@example.com",
                "password", "password",
                "role", "ROLE_USER"
        );

        when(userService.registerUser(anyString(), anyString(), anyString(), anySet()))
                .thenThrow(new RuntimeException("Registration failed"));

        ResponseEntity<?> response = adminController.registerAdminUser(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("User registration failed", ((Map) response.getBody()).get("message"));
        verify(auditService).logAuditEvent(eq("ERROR"), eq("AdminController"), contains("Admin failed to register user"), any(), contains("Registration failed"));
    }

    @SuppressWarnings({ "null", "rawtypes" })
    @Test
    void resetLoginAttempts_Success() {
        doNothing().when(authService).resetLoginAttempts(anyString());

        ResponseEntity<?> response = adminController.resetLoginAttempts("testuser");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Login attempts reset successfully for user testuser", ((Map) response.getBody()).get("message"));
        verify(auditService).logAuditEvent(eq("INFO"), eq("AdminController"), contains("Admin reset login attempts for user"), any(), isNull());
    }

    @SuppressWarnings({ "null", "rawtypes" })
    @Test
    void resetLoginAttempts_Failure() {
        doThrow(new RuntimeException("Reset failed")).when(authService).resetLoginAttempts(anyString());

        ResponseEntity<?> response = adminController.resetLoginAttempts("testuser");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Failed to reset login attempts for user: testuser", ((Map) response.getBody()).get("message"));
        verify(auditService).logAuditEvent(eq("ERROR"), eq("AdminController"), contains("Admin failed to reset login attempts for user"), any(), contains("Reset failed"));
    }

    @SuppressWarnings({ "null", "rawtypes" })
    @Test
    void deleteUser_Success() {
        when(userService.getUserByUsername(anyString())).thenReturn(testUser);
        doNothing().when(refreshTokenService).deleteByUser(any(User.class));
        doNothing().when(userService).deleteUserByUsername(anyString());

        ResponseEntity<?> response = adminController.deleteUser("testuser");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User deleted successfully", ((Map) response.getBody()).get("message"));
        verify(auditService).logAuditEvent(eq("INFO"), eq("AdminController"), contains("Admin deleted user"), any(), isNull());
    }

    @SuppressWarnings({ "null", "rawtypes" })
    @Test
    void deleteUser_UserNotFound() {
        when(userService.getUserByUsername(anyString()))
                .thenThrow(new IllegalArgumentException("User not found"));

        ResponseEntity<?> response = adminController.deleteUser("nonexistentuser");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", ((Map) response.getBody()).get("message"));
        verify(auditService).logAuditEvent(eq("ERROR"), eq("AdminController"), contains("Admin failed to delete user"), any(), contains("User not found"));
    }

    @SuppressWarnings({ "null", "rawtypes" })
    @Test
    void deleteUser_OtherException() {
        when(userService.getUserByUsername(anyString())).thenReturn(testUser);
        doThrow(new RuntimeException("Deletion failed")).when(userService).deleteUserByUsername(anyString());

        ResponseEntity<?> response = adminController.deleteUser("testuser");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Failed to delete user", ((Map) response.getBody()).get("message"));
        verify(auditService).logAuditEvent(eq("ERROR"), eq("AdminController"), contains("Error occurred while deleting user"), any(), contains("Deletion failed"));
    }
}
