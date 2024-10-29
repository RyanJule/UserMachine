package org.machinesystems.UserMachine.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.machinesystems.UserMachine.UserMachineApplication;
import org.machinesystems.UserMachine.config.SecurityConfig;
import org.machinesystems.UserMachine.service.BlacklistedTokenService;
import org.machinesystems.UserMachine.service.CustomUserDetailsService;
import org.machinesystems.UserMachine.controller.TestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {JwtRequestFilter.class, TestController.class})
@ContextConfiguration(classes={UserMachineApplication.class, SecurityConfig.class})
class JwtRequestFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private BlacklistedTokenService blacklistedTokenService;

    private UserDetails userDetails;
    private String token = "testToken";

    @BeforeEach
    void setUp() {
        userDetails = new User("testuser", "password", Set.of(() -> "ROLE_USER"));
    }

    @Test
    void testFilter_ValidToken() throws Exception {
        when(jwtTokenUtil.getUsernameFromToken(token)).thenReturn("testuser");
        when(jwtTokenUtil.validateToken(token, userDetails)).thenReturn(true);
        when(customUserDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(blacklistedTokenService.isTokenBlacklisted(token)).thenReturn(false);

        mockMvc.perform(MockMvcRequestBuilders.get("/some-endpoint")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        verify(jwtTokenUtil).getUsernameFromToken(token);
        verify(customUserDetailsService).loadUserByUsername("testuser");
    }

    @Test
    void testFilter_BlacklistedToken() throws Exception {
        when(blacklistedTokenService.isTokenBlacklisted(token)).thenReturn(true);

        mockMvc.perform(MockMvcRequestBuilders.get("/some-endpoint")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testFilter_NoTokenProvided() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/some-endpoint"))
                .andExpect(status().isForbidden()); // Expecting 403 here if authorization is required

        verifyNoInteractions(jwtTokenUtil, customUserDetailsService, blacklistedTokenService);
    }
}
