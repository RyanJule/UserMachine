package org.machinesystems.UserMachine.service;

import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.repository.UserRepository;
import org.machinesystems.UserMachine.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;

import java.io.UnsupportedEncodingException;
import java.util.Optional;

@Service
public class AuthService {

    private static final int MAX_LOGIN_ATTEMPTS = 5;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserService userService;

    public String loginUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Check if the account is locked
            if (user.isAccountLocked()) {
                throw new IllegalArgumentException("Your account is locked due to too many failed login attempts.");
            }

            // Validate password
            if (passwordEncoder.matches(password, user.getPassword())) {
                // Successful login, reset login attempts
                user.resetLoginAttempts();
                userRepository.save(user);

                return jwtTokenUtil.generateAccessToken(user.getUsername(), user.getRoles());
            } else {
                // Increment login attempts and lock account if necessary
                user.setLoginAttempts(user.getLoginAttempts() + 1);

                if (user.getLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
                    user.setAccountLocked(true);
                }

                userRepository.save(user);
                throw new IllegalArgumentException("Invalid password");
            }
        } else {
            throw new IllegalArgumentException("User not found");
        }
    }

    // Method for admin to reset login attempts
    public void resetLoginAttempts(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.resetLoginAttempts();
        userRepository.save(user);
    }

    public boolean verify(String verificationCode) {
        User user = userRepository.findbyVerificationCode(verificationCode);

        if (user == null || user.isEnabled()) {
            return false;
        } else {
            user.setVerificationCode(null);
            user.setEnabled(true);
            userRepository.save(user);

            return true;
        }
    }
    public void resetPassword(String email, String siteURL) throws MessagingException, UnsupportedEncodingException {
        User user = userService.getUserByEmail(email);
        if (user.isEnabled()) {
            userService.sendPasswordResetLink(email, siteURL);
        }
        else {
            throw new IllegalArgumentException("User not found");
        }
    }

    // Verify token and reset password
    public boolean verifyPasswordReset(String token, String newPassword) {
        return userService.verifyPasswordResetToken(token, newPassword);
    }
}
