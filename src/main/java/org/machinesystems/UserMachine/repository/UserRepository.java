package org.machinesystems.UserMachine.repository;

import org.machinesystems.UserMachine.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.transaction.Transactional;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    @Transactional
    void deleteByUsername(String username);
}
