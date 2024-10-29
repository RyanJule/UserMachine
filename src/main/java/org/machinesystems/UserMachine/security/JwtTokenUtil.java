package org.machinesystems.UserMachine.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Key;
import java.util.Date;
import java.util.Set;

@Component
public class JwtTokenUtil {

    static final String JWT_SECRET_KEY = DotEnvUtil.JWT_SECRET_KEY;  // Use a secure key in production
    // Generate a secret signing key
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET_KEY.getBytes());
    }

    // Generate JWT access token
    public String generateAccessToken(String username, Set<String> roles) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15))  // 15-minute expiration
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract the username from the JWT token
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())  // Use parserBuilder to parse the token
                .build()  // Build the parser
                .parseClaimsJws(token)  // Parse the token and get claims
                .getBody()
                .getSubject();  // Extract the subject (username)
    }

    // Extract the expiration date from the JWT token
    public Date getExpirationDateFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }

    // Validate the token (check expiration and username match)
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Check if the token is expired
    boolean isTokenExpired(String token) {
        return getExpirationDateFromToken(token).before(new Date());
    }
}
