package org.machinesystems.UserMachine.repository;

import org.machinesystems.UserMachine.model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.Optional;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long>{
    Optional<BlacklistedToken> findByToken(String token);

    void deleteByExpirationDateBefore(Date currentDate);

}
