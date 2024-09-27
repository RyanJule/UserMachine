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
import java.util.Set;

@Service
public class AuthService {

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

            if (passwordEncoder.matches(password, user.getPassword())) {
                // Generate token with all roles (not just one role)
                Set<String> roles = user.getRoles();
                
                if (roles == null || roles.isEmpty()) {
                    throw new IllegalArgumentException("User has no roles assigned");
                }
                return jwtTokenUtil.generateAccessToken(user.getUsername(), user.getRoles());
            } else {
                throw new IllegalArgumentException("Invalid password");
            }
        } else {
            throw new IllegalArgumentException("User not found");
        }
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
