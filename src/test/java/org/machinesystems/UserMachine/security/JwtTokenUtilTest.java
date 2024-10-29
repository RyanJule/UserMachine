package org.machinesystems.UserMachine.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenUtilTest {

    private static final String USERNAME = "testuser";
    private static final Set<String> ROLES = Set.of("ROLE_USER");

    @InjectMocks
    private JwtTokenUtil jwtTokenUtil;

    private String token;

    @BeforeEach
    void setUp() {
        jwtTokenUtil = new JwtTokenUtil();

        // Generate a test token
        token = jwtTokenUtil.generateAccessToken(USERNAME, ROLES);
    }

    @Test
    void testGenerateAccessToken() {
        assertEquals(USERNAME, jwtTokenUtil.getUsernameFromToken(token));
    }

    @Test
    void testGetUsernameFromToken() {
        String username = jwtTokenUtil.getUsernameFromToken(token);
        assertEquals(USERNAME, username);
    }

    @Test
    void testGetExpirationDateFromToken() {
        Date expirationDate = jwtTokenUtil.getExpirationDateFromToken(token);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    void testValidateToken() {
        UserDetails userDetails = User.builder()
                .username(USERNAME)
                .password("password")
                .roles("USER")
                .build();

        assertTrue(jwtTokenUtil.validateToken(token, userDetails));
    }

    @Test
    void testValidateToken_InvalidUsername() {
        UserDetails userDetails = User.builder()
                .username("otheruser")
                .password("password")
                .roles("USER")
                .build();

        assertFalse(jwtTokenUtil.validateToken(token, userDetails));
    }

    @Test
    void testIsTokenExpired() {
        // Create a token with a past expiration date to simulate expiration
        String expiredToken = Jwts.builder()
                .setSubject(USERNAME)
                .setExpiration(new Date(System.currentTimeMillis() - 1000))  // Expired 1 second ago
                .signWith(Keys.hmacShaKeyFor(JwtTokenUtil.JWT_SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
                .compact();

        // Catch the ExpiredJwtException to confirm expiration behavior
        boolean isExpired = false;
        try {
            jwtTokenUtil.isTokenExpired(expiredToken);
        } catch (ExpiredJwtException e) {
            isExpired = true;
        }

        assertTrue(isExpired, "Token should be expired");
    }

}
