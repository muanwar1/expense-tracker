package com.speed_anwer.expensetracker.security;

import com.speed_anwer.expensetracker.entity.RevokedToken;
import com.speed_anwer.expensetracker.repository.RevokedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class TokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationService.class);

    private final RevokedTokenRepository revokedTokenRepository;

    public TokenRevocationService(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    /**
     * Marks {@code tokenId} as revoked.
     *
     * <p>Concurrent calls for the same token are safe: the database's UNIQUE constraint on
     * {@code token_id} is the sole arbiter of duplicates. A {@link DataIntegrityViolationException}
     * means a concurrent request already inserted the row, so we treat it as "already revoked"
     * and return normally — no 500 is propagated to the caller.
     */
    @Transactional
    public void revoke(String tokenId, long expiresAtMs) {
        if (tokenId == null) {
            return;
        }
        RevokedToken revokedToken = new RevokedToken();
        revokedToken.setTokenId(tokenId);
        revokedToken.setExpiresAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(expiresAtMs), ZoneId.systemDefault()));
        try {
            revokedTokenRepository.save(revokedToken);
        } catch (DataIntegrityViolationException ex) {
            // Another concurrent request already revoked this token — this is expected and safe.
            log.debug("Token '{}' already revoked (duplicate insert ignored): {}", tokenId, ex.getMessage());
        }
    }

    public boolean isRevoked(String tokenId) {
        return tokenId != null && revokedTokenRepository.existsByTokenId(tokenId);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        revokedTokenRepository.deleteAllExpiredBefore(LocalDateTime.now());
    }
}
