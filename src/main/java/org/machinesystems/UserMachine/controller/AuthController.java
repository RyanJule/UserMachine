package org.machinesystems.UserMachine.controller;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.machinesystems.UserMachine.service.AuthService;
import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.repository.UserRepository;
import org.machinesystems.UserMachine.security.JwtTokenUtil;
import org.machinesystems.UserMachine.service.BlacklistedTokenService;
import org.machinesystems.UserMachine.service.CustomUserDetailsService;
import org.machinesystems.UserMachine.service.RefreshTokenService;
import org.machinesystems.UserMachine.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
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
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private BlacklistedTokenService blacklistedTokenService;

    @Autowired
    private UserService userService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private AuthService authService;

    // Register a new user with default role "ROLE_USER"
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) 
            throws UnsupportedEncodingException, MessagingException {
        
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");

        // Register the user with default "ROLE_USER"
        User newUser = userService.registerUser(username, email, password, Set.of("ROLE_USER"));

        return ResponseEntity.status(201).body(Map.of(
                "message", "User registered successfully",
                "username", newUser.getUsername()
        ));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyUser(@Param("code") String code) {
        if (authService.verify(code)) {
            return ResponseEntity.status(201).body(Map.of(
                "message", "User verified"
            ));
        } else {
            return ResponseEntity.status(400).body(Map.of(
                "message", "User not verified"
            ));
        }
    }

    // Login user and generate JWT tokens
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        try {
            // Get the user to check login attempts
            User user = userService.getUserByUsername(username);
            
            // Check if the user has exceeded maximum login attempts
            if (user.getLoginAttempts() >= 5) {
                user.setAccountLocked(true);
                userRepository.save(user);
                return ResponseEntity.status(403)
                    .body(Map.of("message", "User account locked due to too many failed login attempts. Please contact support."));
            }

            // Authenticate the user
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

            // Load user details
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

            if (!userDetails.isEnabled()) {
                return ResponseEntity.status(400)
                    .body(Map.of("message", "User is not authenticated"));
            }

            // Reset login attempts on successful login
            userService.resetLoginAttempts(username);

            // Convert GrantedAuthority to Set<String> for roles
            Set<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            // Generate access token and refresh token
            String accessToken = jwtTokenUtil.generateAccessToken(userDetails.getUsername(), roles);
            String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken
            ));

        } catch (Exception e) {
            e.printStackTrace();  // Add better logging in production

            // Increment failed login attempts
            userService.incrementLoginAttempts(username);

            return ResponseEntity.status(401).body(Map.of("message", "Invalid username or password"));
        }
    }

    // Token refresh
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshTokenStr = request.get("refreshToken");

        try {
            // Verify and find the refresh token
            var refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                    .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

            // Check if the refresh token is expired
            refreshTokenService.verifyExpiration(refreshToken);

            // Get the user associated with the refresh token
            User user = refreshToken.getUser();

            // Blacklist the current access token (assumed to be passed in the header)
            String oldAccessToken = request.get("accessToken");
            Date oldAccessTokenExpirationDate = jwtTokenUtil.getExpirationDateFromToken(oldAccessToken);
            blacklistedTokenService.blacklistToken(oldAccessToken, oldAccessTokenExpirationDate);

            // Generate a new access token
            Set<String> roles = user.getRoles();
            String newAccessToken = jwtTokenUtil.generateAccessToken(user.getUsername(), roles);

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "refreshToken", refreshTokenStr
            ));
        } 
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Token refresh failed: " + e.getMessage()));
        }
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String refreshTokenStr = request.get("refreshToken");
        String accessTokenStr = request.get("accessToken");
    
        // Check if both tokens are provided
        if (refreshTokenStr == null || accessTokenStr == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Both access token and refresh token are required for logout"));
        }
    
        try {
            // Authenticate the user using the refresh token
            var refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                    .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
    
            // Verify if the refresh token is expired
            refreshTokenService.verifyExpiration(refreshToken);
    
            // Get the associated user
            User user = refreshToken.getUser();
    
            // Use the expired token method to get the username from the access token
            String usernameFromAccessToken = jwtTokenUtil.getUsernameFromToken(accessTokenStr);
            if (!usernameFromAccessToken.equals(user.getUsername())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Access token does not belong to the user"));
            }
    
            // Blacklist the access token (even if expired)
            Date accessTokenExpirationDate = jwtTokenUtil.getExpirationDateFromToken(accessTokenStr);
            blacklistedTokenService.blacklistToken(accessTokenStr, accessTokenExpirationDate);
    
            // Delete or invalidate the refresh token
            refreshTokenService.deleteByUser(user);
    
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Logout failed: " + e.getMessage()));
        }
    }
    // requests password reset
    @PostMapping("/reset-password-request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        try {
            authService.resetPassword(email, "http://localhost:8080");
            return ResponseEntity.ok(Map.of("message", "Password reset email sent"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("message", "Error sending password reset email"));
        }
    }

    // Reset password
    // Serve the reset password form
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token) {
        // Simple HTML form to submit the new password
        return "<html>" +
                "<body>" +
                "<h2>Reset Password</h2>" +
                "<form action='/auth/reset-password' method='post'>" +
                "<input type='hidden' name='token' value='" + token + "' />" + // Hidden field for token
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
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } else {
            return ResponseEntity.status(400).body(Map.of("message", "Invalid or expired token"));
        }
    }
}
