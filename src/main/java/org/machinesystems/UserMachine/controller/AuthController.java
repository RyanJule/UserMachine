package org.machinesystems.UserMachine.controller;

import org.machinesystems.UserMachine.model.RefreshToken;
import org.machinesystems.UserMachine.security.JwtTokenUtil;
import org.machinesystems.UserMachine.service.RefreshTokenService;
import org.machinesystems.UserMachine.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserService userService;

    // Login method (omitted for brevity)

    // Token refresh endpoint
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshTokenStr = request.get("refreshToken");

        // Find and verify the refresh token
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        // Check if refresh token is expired
        refreshTokenService.verifyExpiration(refreshToken);

        // Generate a new access token
        String newAccessToken = jwtTokenUtil.generateAccessToken(
                refreshToken.getUser().getUsername(), refreshToken.getUser().getRoles());

        // Return the new access token
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    // Logout method (invalidate refresh tokens)
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String username = request.get("username");

        // Find the user by username
        var user = userService.getUserByUsername(username);

        // Delete refresh token for the user
        refreshTokenService.deleteByUser(user);

        return ResponseEntity.ok("Logged out successfully");
    }
}
