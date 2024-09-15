package org.machinesystems.UserMachine.controller;

import org.machinesystems.UserMachine.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public String loginUser(@RequestParam String username, @RequestParam String password) {
        try {
            return authService.loginUser(username, password);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}