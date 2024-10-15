package org.machinesystems.UserMachine.controller;

import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.service.AuthService;
import org.machinesystems.UserMachine.service.RefreshTokenService;
import org.machinesystems.UserMachine.service.UserService;
import org.machinesystems.UserMachine.service.AuditService; // Import AuditService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.mail.MessagingException;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private AuditService auditService;  // Inject AuditService

    // Endpoint for admin to register a new user with specified roles
    @PostMapping("/register")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> registerAdminUser(@RequestBody Map<String, String> request) 
            throws UnsupportedEncodingException, MessagingException {
        
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");
        String role = request.get("role");

        Set<String> roles = Set.of(role);  // For now, only one role is passed

        try {
            // Register the user
            User newUser = userService.registerUser(username, email, password, roles);

            // Log the successful registration
            auditService.logAuditEvent("INFO", "AdminController", "Admin registered new user: " + username, Thread.currentThread().getName(), null);

            return ResponseEntity.status(201).body(Map.of(
                    "message", "User registered successfully",
                    "username", newUser.getUsername(),
                    "roles", newUser.getRoles()
            ));
        } catch (Exception e) {
            // Log the failure in case of an exception
            auditService.logAuditEvent("ERROR", "AdminController", "Admin failed to register user: " + username, Thread.currentThread().getName(), e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "User registration failed"));
        }
    }

    // Endpoint to reset login attempts for a user (only accessible by Admin)
    @PostMapping("/reset-login-attempts/{username}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> resetLoginAttempts(@PathVariable String username) {
        try {
            authService.resetLoginAttempts(username);
            
            // Log successful login attempts reset
            auditService.logAuditEvent("INFO", "AdminController", "Admin reset login attempts for user: " + username, Thread.currentThread().getName(), null);

            return ResponseEntity.ok(Map.of("message", "Login attempts reset successfully for user " + username));
        } catch (Exception e) {
            // Log the failure in case of an exception
            auditService.logAuditEvent("ERROR", "AdminController", "Admin failed to reset login attempts for user: " + username, Thread.currentThread().getName(), e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Failed to reset login attempts for user: " + username));
        }
    }

    // Admin delete user by username
    @DeleteMapping("/delete/{username}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        try {
            // Call the service to delete the user
            User user = userService.getUserByUsername(username);
            refreshTokenService.deleteByUser(user);
            userService.deleteUserByUsername(username);
            
            // Log successful user deletion
            auditService.logAuditEvent("INFO", "AdminController", "Admin deleted user: " + username, Thread.currentThread().getName(), null);

            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (IllegalArgumentException e) {
            // Log the failure in case the user was not found
            auditService.logAuditEvent("ERROR", "AdminController", "Admin failed to delete user: " + username, Thread.currentThread().getName(), e.getMessage());
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Log any other failures
            auditService.logAuditEvent("ERROR", "AdminController", "Error occurred while deleting user: " + username, Thread.currentThread().getName(), e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Failed to delete user"));
        }
    }
}
