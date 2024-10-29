package org.machinesystems.UserMachine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.verify;

public class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Mock
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSendSimpleEmail() {
        // Given
        String to = "test@example.com";
        String subject = "Test Subject";
        String text = "Test Email Body";

        // When
        emailService.sendSimpleEmail(to, subject, text);

        // Then
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        verify(mailSender).send(message);
    }

    @Test
    void testSendVerificationEmail() {
        // Given
        String to = "test@example.com";
        String verificationToken = "testToken";

        // When
        emailService.sendVerificationEmail(to, verificationToken);

        // Then
        String expectedSubject = "Verify Your Email";
        String expectedVerificationUrl = "https://yourapp.com/verify?token=" + verificationToken;
        String expectedText = "Please click the following link to verify your email: " + expectedVerificationUrl;

        // Verify that sendSimpleEmail is called with the expected parameters
        verify(mailSender).send(verifyMessage(to, expectedSubject, expectedText));
    }

    private SimpleMailMessage verifyMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        return message;
    }
}
