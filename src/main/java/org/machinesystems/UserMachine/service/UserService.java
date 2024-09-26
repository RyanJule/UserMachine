package org.machinesystems.UserMachine.service;

import java.io.UnsupportedEncodingException;
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
    public User registerUser(String username, String email, String password) 
            throws UnsupportedEncodingException, MessagingException {
        if (userRepository.findByUsername(username).isPresent() || userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Username or email already exists");
        }

        String randomCode = RandomString.make(64);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(Set.of("ROLE_USER"));
        user.setVerificationCode(randomCode);
        user.setEnabled(false);
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
    
}
