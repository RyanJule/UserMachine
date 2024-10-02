package org.machinesystems.UserMachine.service;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.repository.UserRepository;
import org.machinesystems.UserMachine.security.DotEnvUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import net.bytebuddy.utility.RandomString;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;
    static final String EMAIL = DotEnvUtil.EMAIL;
    
    // Register a new user
    // Register a new user with specified roles
    public User registerUser(String username, String email, String password, Set<String> roles) 
            throws UnsupportedEncodingException, MessagingException {
        if (userRepository.findByUsername(username).isPresent() || userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Username or email already exists");
        }

        String randomCode = RandomString.make(64);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(roles != null && !roles.isEmpty() ? roles : Set.of("ROLE_USER"));  // Default to "ROLE_USER"
        user.setVerificationCode(randomCode);
        user.setEnabled(false);
        
        // Send verification email
        sendVerificationEmail(user, "http://localhost:8080/auth");
        userRepository.save(user);

        return user;
    }

    private void sendVerificationEmail(User user, String siteURL)
            throws MessagingException, UnsupportedEncodingException {
        String toAddress = user.getEmail();
        String fromAddress = EMAIL;
        String senderName = "Your company name";
        String subject = "Please verify your registration";
        String content = "Dear [[name]],<br>"
                + "Please click the link below to verify your registration:<br>"
                + "<h3><a href=\"[[URL]]\" target=\"_self\">VERIFY</a></h3>"
                + "Thank you,<br>"
                + "Your company name.";
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        
        helper.setFrom(fromAddress, senderName);
        helper.setTo(toAddress);
        helper.setSubject(subject);
        
        content = content.replace("[[name]]", user.getUsername());
        String verifyURL = siteURL + "/verify?code=" + user.getVerificationCode();
        
        content = content.replace("[[URL]]", verifyURL);
        
        helper.setText(content, true);
        
        mailSender.send(message);
        
    }

    // Get user by username
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    // Get user by username
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    // Update user profile (username and email)
    public User updateUserProfile(String username, String newUsername, String newEmail) {
        User user = getUserByUsername(username);

        // Check if new username or email already exists for another user
        if (!newUsername.equals(user.getUsername()) && userRepository.findByUsername(newUsername).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (!newEmail.equals(user.getEmail()) && userRepository.findByEmail(newEmail).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Update username and email
        user.setUsername(newUsername);
        user.setEmail(newEmail);
        return userRepository.save(user);
    }

    // Change user password (re-hash the new password)
    public User changePassword(String username, String currentPassword, String newPassword) {
        User user = getUserByUsername(username);

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Set and save new password
        user.setPassword(passwordEncoder.encode(newPassword));

        return userRepository.save(user);
    }

    public void deleteUserByUsername(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            userRepository.deleteByUsername(username);
        } else {
            throw new IllegalArgumentException("User not found");
        }
    }
    public void sendPasswordResetLink(String email, String siteURL) throws UnsupportedEncodingException, MessagingException {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Email not found");
        }

        User user = userOpt.get();
        String resetToken = RandomString.make(64);
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordExpiry(new Date(System.currentTimeMillis() + 15 * 60 * 1000)); // 15 minutes expiry

        userRepository.save(user);
        sendPasswordResetEmail(user, siteURL);
    }

    private void sendPasswordResetEmail(User user, String siteURL) throws MessagingException, UnsupportedEncodingException {
        String toAddress = user.getEmail();
        String fromAddress = "youremail@example.com";
        String senderName = "Your Company Name";
        String subject = "Reset your password";
        String content = "Dear [[name]],<br>"
                + "You have requested to reset your password.<br>"
                + "Click the link below to reset your password:<br>"
                + "<h3><a href=\"[[URL]]\" target=\"_self\">RESET PASSWORD</a></h3>"
                + "Ignore this email if you do not want to change your password.<br>"
                + "Thank you,<br>"
                + "Your Company Name.";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(fromAddress, senderName);
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", user.getUsername());
        String resetURL = siteURL + "/auth/reset-password?token=" + user.getResetPasswordToken();
        content = content.replace("[[URL]]", resetURL);

        helper.setText(content, true);

        mailSender.send(message);
    }

    // Verify password reset token and reset the password
    public boolean verifyPasswordResetToken(String token, String newPassword) {
        Optional<User> userOpt = userRepository.findByResetPasswordToken(token);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // Check if the token is still valid
        if (user.getResetPasswordExpiry().before(new Date())) {
            return false;
        }

        // Reset the password and clear the token
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiry(null);

        userRepository.save(user);

        return true;
    }

    // Increment login attempts for a user
    public void incrementLoginAttempts(String username) {
        User user = getUserByUsername(username);
        int currentAttempts = user.getLoginAttempts();
        user.setLoginAttempts(currentAttempts + 1);
        userRepository.save(user);
    }

    // Reset login attempts for a user
    public void resetLoginAttempts(String username) {
        User user = getUserByUsername(username);
        user.setLoginAttempts(0);
        userRepository.save(user);
    }

}
