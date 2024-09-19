package org.machinesystems.UserMachine.controller;

import java.util.Set;
import java.util.stream.Collectors;
import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.security.JwtTokenUtil;
import org.machinesystems.UserMachine.service.CustomUserDetailsService;
import org.machinesystems.UserMachine.service.RefreshTokenService;
import org.machinesystems.UserMachine.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    // Register a new user
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");

        // Register user using UserService
        User newUser = userService.registerUser(username, email, password);

        return ResponseEntity.status(201).body(Map.of(
                "message", "User registered successfully",
                "username", newUser.getUsername()
        ));
    }

    // Login user and generate JWT tokens
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
    
        try {
            // Authenticate the user
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    
            // Load user details
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
    
            // Convert GrantedAuthority to Set<String> for roles
            Set<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
    
            // Generate access token and refresh token
            String accessToken = jwtTokenUtil.generateAccessToken(userDetails.getUsername(), roles);
            String refreshToken = refreshTokenService.createRefreshToken(userService.getUserByUsername(username)).getToken();
    
            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }
        // Token refresh
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshTokenStr = request.get("refreshToken");

        // Find and verify the refresh token
        var refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        // Check if refresh token is expired
        refreshTokenService.verifyExpiration(refreshToken);

        // Generate a new access token
        String newAccessToken = jwtTokenUtil.generateAccessToken(
                refreshToken.getUser().getUsername(), refreshToken.getUser().getRoles());

        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    // Logout and invalidate refresh tokens
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String username = request.get("username");

        // Find the user by username and delete their refresh token
        var user = userService.getUserByUsername(username);
        refreshTokenService.deleteByUser(user);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
