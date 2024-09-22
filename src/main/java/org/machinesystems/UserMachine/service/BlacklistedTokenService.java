package org.machinesystems.UserMachine.service;

import org.machinesystems.UserMachine.model.BlacklistedToken;
import org.machinesystems.UserMachine.repository.BlacklistedTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class BlacklistedTokenService {

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    public void blacklistToken(String token, Date expirationDate){
        blacklistedTokenRepository.save(new BlacklistedToken(token, expirationDate));
    }

    public boolean isTokenBlacklisted(String token) {
        Optional<BlacklistedToken> blacklistedToken = blacklistedTokenRepository.findByToken(token);
        return blacklistedToken.isPresent();
    }

    public void cleanUpExpiredTokens() {
        Date now = new Date();
        blacklistedTokenRepository.deleteByExpirationDateBefore(now);
    }
}
