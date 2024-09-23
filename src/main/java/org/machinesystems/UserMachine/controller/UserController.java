package org.machinesystems.UserMachine.controller;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.security.JwtTokenUtil;
import org.machinesystems.UserMachine.service.BlacklistedTokenService;
import org.machinesystems.UserMachine.service.RefreshTokenService;
import org.machinesystems.UserMachine.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private BlacklistedTokenService blacklistedTokenService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

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

    // Delete user by username (requires valid access token)
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authorizationHeader)  {

        try {
            String username = userDetails.getUsername();

            // Delete the user's refresh token
            User user = userService.getUserByUsername(username);
            refreshTokenService.deleteByUser(user);

            // Blacklist the current access token
            String accessToken = authorizationHeader.substring(7);  // Remove "Bearer " prefix
            Date accessTokenExpirationDate = jwtTokenUtil.getExpirationDateFromToken(accessToken);
            blacklistedTokenService.blacklistToken(accessToken, accessTokenExpirationDate);

            // Delete the user from the database
            userService.deleteUserByUsername(username);

            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete user"));
        }
    }

}
