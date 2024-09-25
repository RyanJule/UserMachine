package org.machinesystems.UserMachine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.repository.UserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;  // Mock the mail sender

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize a test user object
        user = new User();
        user.setUsername("john");
        user.setEmail("john@example.com");
        user.setPassword("password");  // Plain text password for testing
    }

    @Test
    void testRegisterUser_Success() throws UnsupportedEncodingException, MessagingException {
        // Mock behavior for existing checks and password encoding
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

        // Mock the MimeMessage creation
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Mock the behavior for the MimeMessageHelper
        doNothing().when(mailSender).send(any(MimeMessage.class));  // Mock the sending email

        // Use Mockito's Answer to return the same user passed into save()
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Call the service
        User registeredUser = userService.registerUser("john", "john@example.com", "password");

        // Verify that the password was encoded
        assertEquals("hashedPassword", registeredUser.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterUser_UserExists() {
        // Mock the scenario where the username already exists
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(new User()));

        // Test that the service throws an exception
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser("john", "john@example.com", "password");
        });
    }
}
