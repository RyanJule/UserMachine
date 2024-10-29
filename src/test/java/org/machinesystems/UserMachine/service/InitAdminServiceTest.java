package org.machinesystems.UserMachine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.repository.UserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InitAdminServiceTest {

    @InjectMocks
    private InitAdminService initAdminService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testInitAdminUser_AdminNotExists() {
        // Given
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin123")).thenReturn("encodedPassword");

        // When
        initAdminService.initAdminUser(); // Directly call the method for testing

        // Then
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(user -> 
            user.getUsername().equals("admin") &&
            user.getEmail().equals("admin@example.com") &&
            user.getPassword().equals("encodedPassword") &&
            user.getRoles().contains("ROLE_ADMIN") &&
            user.isEnabled()
        ));
    }

    @Test
    void testInitAdminUser_AdminExists() {
        // Given
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User()));

        // When
        initAdminService.initAdminUser(); // Directly call the method for testing

        // Then
        verify(userRepository).findByUsername("admin");
    }
}

