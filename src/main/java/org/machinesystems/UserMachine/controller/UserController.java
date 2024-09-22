package org.machinesystems.UserMachine.controller;

import org.machinesystems.UserMachine.service.BlacklistedTokenService;
import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.service.UserService;
import org.machinesystems.UserMachine.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.Date;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private BlacklistedTokenService blacklistedTokenService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    // Update user profile (username and email)
    // Update user profile (username and email)
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Map<String, String> request) {
        try {
            // Extract current username and the new data
            String currentUsername = userDetails.getUsername();
            String newUsername = request.get("username");
            String newEmail = request.get("email");

            // Check if the username is being updated
            boolean usernameChanged = !newUsername.equals(currentUsername);

            // Update the user's profile
            User updatedUser = userService.updateUserProfile(currentUsername, newUsername, newEmail);

            // Blacklist the old token if the username changed
            if (usernameChanged) {
                String oldAccessToken = authorizationHeader.substring(7);  // Remove "Bearer " prefix
                Date accessTokenExpirationDate = jwtTokenUtil.getExpirationDateFromToken(oldAccessToken);
                blacklistedTokenService.blacklistToken(oldAccessToken, accessTokenExpirationDate);

                // Generate a new access token with the updated username
                Set<String> roles = updatedUser.getRoles();
                String newAccessToken = jwtTokenUtil.generateAccessToken(newUsername, roles);

                // Return updated user info along with the new access token
                return ResponseEntity.ok(Map.of(
                        "user", updatedUser,
                        "accessToken", newAccessToken
                ));
            }

        // If username didn't change, return the updated user
        return ResponseEntity.ok(updatedUser);

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Profile update failed"));
    }
}


    // Change user password
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {

        String currentUsername = userDetails.getUsername();
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        // Change the user's password
        userService.changePassword(currentUsername, currentPassword, newPassword);

        return ResponseEntity.ok("Password updated successfully");
    }
}
