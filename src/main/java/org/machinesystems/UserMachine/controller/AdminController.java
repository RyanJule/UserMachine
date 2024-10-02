package org.machinesystems.UserMachine.controller;

import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.service.AuthService;
import org.machinesystems.UserMachine.service.RefreshTokenService;
import org.machinesystems.UserMachine.service.UserService;
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

import java.util.Map;
import java.util.Set;
import java.io.UnsupportedEncodingException;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenService refreshTokenService;

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
        
        User newUser = userService.registerUser(username, email, password, roles);

        return ResponseEntity.status(201).body(Map.of(
                "message", "User registered successfully",
                "username", newUser.getUsername(),
                "roles", newUser.getRoles()
        ));
    }

    // Endpoint to reset login attempts for a user (only accessible by Admin)
    @PostMapping("/reset-login-attempts/{username}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> resetLoginAttempts(@PathVariable String username) {
        authService.resetLoginAttempts(username);
        return ResponseEntity.ok(Map.of("message", "Login attempts reset successfully for user " + username));
    }

    //to-do: admins can delete users
    // Admin delete user by username
    @DeleteMapping("/delete/{username}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        try {
            // Call the service to delete user by username
            User user = userService.getUserByUsername(username);
            refreshTokenService.deleteByUser(user);
            userService.deleteUserByUsername(username);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (IllegalArgumentException e) {
            // Handle user not found case
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }
}