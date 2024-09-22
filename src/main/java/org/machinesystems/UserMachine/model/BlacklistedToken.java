package org.machinesystems.UserMachine.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "blacklisted_tokens")
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Date expirationDate;

    // Constructors, getters, and setters
    public BlacklistedToken() {}

    public BlacklistedToken(String token, Date expirationDate) {
        this.token = token;
        this.expirationDate = expirationDate;
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }
}