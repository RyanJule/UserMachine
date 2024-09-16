package org.machinesystems.UserMachine.service;

import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.repository.UserRepository;
import org.machinesystems.UserMachine.security.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize the test user object
        user = new User();
        user.setUsername("john");
        user.setPassword("hashedPassword"); // Simulate a hashed password
        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER"); // Ensure the user has at least one role
        user.setRoles(roles);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testLoginUser_Success() {
        // Mock repository to return user when queried
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        // Mock passwordEncoder to match password
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        // Mock JWT token generation
        when(jwtTokenUtil.generateAccessToken(anyString(), any(Set.class))).thenReturn("mockJwtToken");

        // Call the login service
        String token = authService.loginUser("john", "password");

        // Verify the result
        assertEquals("mockJwtToken", token);
        verify(userRepository).findByUsername("john");
    }

    @Test
    void testLoginUser_InvalidPassword() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false); // Incorrect password

        assertThrows(IllegalArgumentException.class, () -> {
            authService.loginUser("john", "wrongPassword");
        });
    }

    @Test
    void testLoginUser_UserNotFound() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty()); // User not found

        assertThrows(IllegalArgumentException.class, () -> {
            authService.loginUser("nonexistentUser", "password");
        });
    }

    @Test
    void testLoginUser_NoRolesAssigned() {
        // Set user with no roles
        user.setRoles(new HashSet<>());

        // Mock repository to return user
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // Test for no roles assigned
        assertThrows(IllegalArgumentException.class, () -> {
            authService.loginUser("john", "password");
        });
    }
}