package org.machinesystems.UserMachine.controller;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.machinesystems.UserMachine.service.AuthService;
import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.security.JwtTokenUtil;
import org.machinesystems.UserMachine.service.BlacklistedTokenService;
import org.machinesystems.UserMachine.service.RefreshTokenService;
import org.machinesystems.UserMachine.service.UserService;
import org.machinesystems.UserMachine.service.AuditService;  // Import the AuditService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.mail.MessagingException;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private BlacklistedTokenService blacklistedTokenService;

    @Autowired
    private UserService userService;


    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private AuthService authService;

    @Autowired
    private AuditService auditService;  // Inject the AuditService

    // Register a new user with default role "ROLE_USER"
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) 
            throws UnsupportedEncodingException, MessagingException {
        
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");

        try {
            // Register the user with default "ROLE_USER"
            User newUser = userService.registerUser(username, email, password, Set.of("ROLE_USER"));

            // Log audit event for successful registration
            auditService.logAuditEvent("INFO", "AuthController", "User registered: " + username, Thread.currentThread().getName(), null);

            return ResponseEntity.status(201).body(Map.of(
                    "message", "User registered successfully",
                    "username", newUser.getUsername()
            ));
        } catch (Exception e) {
            auditService.logAuditEvent("ERROR", "AuthController", "User registration failed for: " + username, Thread.currentThread().getName(), e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "User registration failed"));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyUser(@Param("code") String code) {
        if (authService.verify(code)) {
            auditService.logAuditEvent("INFO", "AuthController", "User verified with code: " + code, Thread.currentThread().getName(), null);
            return ResponseEntity.status(201).body(Map.of("message", "User verified"));
        } else {
            auditService.logAuditEvent("ERROR", "AuthController", "User verification failed with code: " + code, Thread.currentThread().getName(), null);
            return ResponseEntity.status(400).body(Map.of("message", "User not verified"));
        }
    }

    // Login user and generate JWT tokens
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
    
        try {
            // Call AuthService to handle the login logic
            String accessToken = authService.loginUser(username, password);
            User user = userService.getUserByUsername(username);
    
            // Generate refresh token and log successful login
            String refreshToken = refreshTokenService.createRefreshToken(user).getToken();
            auditService.logAuditEvent("INFO", "AuthController", "User logged in: " + username, Thread.currentThread().getName(), null);
    
            return ResponseEntity.ok(Map.of("accessToken", accessToken, "refreshToken", refreshToken));
    
        } catch (IllegalArgumentException e) {
            // Log error if login fails
            auditService.logAuditEvent("ERROR", "AuthController", "Login failed: " + username, Thread.currentThread().getName(), e.getMessage());
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
    }
    // Token refresh
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshTokenStr = request.get("refreshToken");

        try {
            var refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                    .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

            refreshTokenService.verifyExpiration(refreshToken);

            User user = refreshToken.getUser();

            String oldAccessToken = request.get("accessToken");
            Date oldAccessTokenExpirationDate = jwtTokenUtil.getExpirationDateFromToken(oldAccessToken);
            blacklistedTokenService.blacklistToken(oldAccessToken, oldAccessTokenExpirationDate);

            Set<String> roles = user.getRoles();
            String newAccessToken = jwtTokenUtil.generateAccessToken(user.getUsername(), roles);

            auditService.logAuditEvent("INFO", "AuthController", "Token refreshed for user: " + user.getUsername(), Thread.currentThread().getName(), null);

            return ResponseEntity.ok(Map.of("accessToken", newAccessToken, "refreshToken", refreshTokenStr));
        } catch (Exception e) {
            auditService.logAuditEvent("ERROR", "AuthController", "Token refresh failed", Thread.currentThread().getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Token refresh failed: " + e.getMessage()));
        }
    }


    // Logout user
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String refreshTokenStr = request.get("refreshToken");
        String accessTokenStr = request.get("accessToken");

        if (refreshTokenStr == null || accessTokenStr == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Both access token and refresh token are required for logout"));
        }

        try {
            var refreshToken = refreshTokenService.findByToken(refreshTokenStr).orElseThrow(() -> new RuntimeException("Invalid refresh token"));

            refreshTokenService.verifyExpiration(refreshToken);

            User user = refreshToken.getUser();

            // Verify that the access token belongs to the user
            String usernameFromAccessToken = jwtTokenUtil.getUsernameFromToken(accessTokenStr);
            if (!usernameFromAccessToken.equals(user.getUsername())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Access token does not belong to the user"));
            }

            // Blacklist the access token
            Date accessTokenExpirationDate = jwtTokenUtil.getExpirationDateFromToken(accessTokenStr);
            blacklistedTokenService.blacklistToken(accessTokenStr, accessTokenExpirationDate);

            // Delete the refresh token
            refreshTokenService.deleteByUser(user);

            auditService.logAuditEvent("INFO", "AuthController", "User logged out: " + user.getUsername(), Thread.currentThread().getName(), null);

            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));

        } catch (Exception e) {
            auditService.logAuditEvent("ERROR", "AuthController", "Logout failed", Thread.currentThread().getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Logout failed: " + e.getMessage()));
        }
    }

    // requests password reset
    @PostMapping("/reset-password-request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        try {
            authService.resetPassword(email, "http://localhost:8080");
            auditService.logAuditEvent("INFO", "AuthController", "Password reset requested for: " + email, Thread.currentThread().getName(), null);
            return ResponseEntity.ok(Map.of("message", "Password reset email sent"));
        } catch (Exception e) {
            auditService.logAuditEvent("ERROR", "AuthController", "Password reset request failed for: " + email, Thread.currentThread().getName(), e.getMessage());
            return ResponseEntity.status(400).body(Map.of("message", "Error sending password reset email"));
        }
    }

    // Serve the reset password form
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token) {
        return "<html>" +
                "<body>" +
                "<h2>Reset Password</h2>" +
                "<form action='/auth/reset-password' method='post'>" +
                "<input type='hidden' name='token' value='" + token + "' />" +
                "<label for='newPassword'>New Password:</label><br/>" +
                "<input type='password' id='newPassword' name='newPassword'><br/>" +
                "<button type='submit'>Reset Password</button>" +
                "</form>" +
                "</body>" +
                "</html>";
    }

    // Process the password reset
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam("token") String token, @RequestParam("newPassword") String newPassword) {
        if (authService.verifyPasswordReset(token, newPassword)) {
            auditService.logAuditEvent("INFO", "AuthController", "Password reset for token: " + token, Thread.currentThread().getName(), null);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } else {
            auditService.logAuditEvent("ERROR", "AuthController", "Password reset failed for token: " + token, Thread.currentThread().getName(), null);
            return ResponseEntity.status(400).body(Map.of("message", "Invalid or expired token"));
        }
    }
}

