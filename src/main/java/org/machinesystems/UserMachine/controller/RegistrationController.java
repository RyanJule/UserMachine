package org.machinesystems.UserMachine.controller;

import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class RegistrationController {

    @Autowired
    private UserService userService;

    // Endpoint for user registration
    @PostMapping("/register")
    public String registerUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password) {
        try {
            User user = userService.registerUser(username, email, password);
            return "User registered successfully: " + user.getUsername();
        } catch (IllegalArgumentException e) {
            return e.getMessage(); // Return error message if registration fails
        }
    }
}