package org.machinesystems.UserMachine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.machinesystems.UserMachine.model.User;
import org.machinesystems.UserMachine.repository.UserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize a test user with username, password, and roles
        user = new User();
        user.setUsername("john");
        user.setPassword("hashedPassword");

        // Add roles
        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER");
        roles.add("ROLE_ADMIN");
        user.setRoles(roles);
    }

    @Test
    void testLoadUserByUsername_Success() {
        // Mock repository to return the user when queried by username
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        // Call the service method
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john");

        // Assertions
        assertEquals("john", userDetails.getUsername());
        assertEquals("hashedPassword", userDetails.getPassword());

        // Verify that roles were correctly mapped to GrantedAuthority
        assertEquals(2, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_USER")));
        assertTrue(userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN")));

        // Verify that the repository was called with the correct username
        verify(userRepository).findByUsername("john");
    }

    @Test
    void testLoadUserByUsername_UserNotFound() {
        // Mock repository to return an empty result
        when(userRepository.findByUsername("invalidUser")).thenReturn(Optional.empty());

        // Expect UsernameNotFoundException
        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("invalidUser");
        });

        // Verify the repository was called
        verify(userRepository).findByUsername("invalidUser");
    }
}

