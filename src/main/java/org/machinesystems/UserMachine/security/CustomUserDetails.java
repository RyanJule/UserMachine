package org.machinesystems.UserMachine.security;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.machinesystems.UserMachine.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails{

    private User user;
    
    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Convert the user's roles (Set<String>) into GrantedAuthority objects for Spring Security
        Set<String> roles = user.getRoles();
        return roles.stream()
                .map(SimpleGrantedAuthority::new) // Convert each role into SimpleGrantedAuthority
                .collect(Collectors.toSet());
    }


    @Override
    public String getPassword() {
        // Return the user's password (hashed in the database)
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // Return the user's username
        return user.getUsername();
    }
}
