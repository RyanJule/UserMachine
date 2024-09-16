package org.machinesystems.UserMachine.repository;

import org.machinesystems.UserMachine.model.RefreshToken;
import org.machinesystems.UserMachine.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
}