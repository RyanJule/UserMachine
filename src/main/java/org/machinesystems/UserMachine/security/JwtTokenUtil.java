package org.machinesystems.UserMachine.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.Set;

@Component
public class JwtTokenUtil {

    private static final String SECRET_KEY = "ImeanThisIsreallyquiteabigsecretkeyim69notsure89if1111weneedittobelongerbutherewego";  // Use a secure key in production
    private static final long ACCESS_TOKEN_EXPIRATION = 900000; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 86400000L; // 1 day (24 hours)

    // Generate signing key
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // Generate Access Token with user roles
    public String generateAccessToken(String username, Set<String> roles) {
        Map<String, Object> claims = Map.of("roles", roles);  // Store roles as a claim

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Generate Refresh Token (no roles required)
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract username from JWT token
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // Extract roles from Access Token
    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        return (Set<String>) Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("roles", Set.class);
    }

    // Validate JWT token (for both access and refresh tokens)
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Check if a token is expired
    public boolean isTokenExpired(String token) {
        Date expiration = Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }

    // Validate that the token is a refresh token (no roles should be present)
    public boolean isRefreshToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Refresh token should not contain roles
        return claims.get("roles") == null;
    }
}
