package org.machinesystems.UserMachine.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;

    public void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    public void sendVerificationEmail(String to, String verificationToken) {
        String subject = "Verify Your Email";
        String verificationUrl = "https://yourapp.com/verify?token=" + verificationToken;
        String text = "Please click the following link to verify your email: " + verificationUrl;

        sendSimpleEmail(to, subject, text);
    }
}
