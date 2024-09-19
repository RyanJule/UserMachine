package org.machinesystems.UserMachine.controller;

import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    // Update user profile (username and email)
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        
        String currentUsername = userDetails.getUsername();
        String newUsername = request.get("username");
        String newEmail = request.get("email");

        // Update the user's profile
        User updatedUser = userService.updateUserProfile(currentUsername, newUsername, newEmail);

        return ResponseEntity.ok(updatedUser);
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
