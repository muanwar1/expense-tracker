package com.speed_anwer.expensetracker.security;

import com.speed_anwer.expensetracker.entity.RevokedToken;
import com.speed_anwer.expensetracker.repository.RevokedTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenRevocationServiceTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    private TokenRevocationService tokenRevocationService;

    @BeforeEach
    void setUp() {
        tokenRevocationService = new TokenRevocationService(revokedTokenRepository);
    }

    // --- revoke() ---

    @Test
    void revoke_nullTokenId_doesNothing() {
        tokenRevocationService.revoke(null, System.currentTimeMillis() + 60_000);

        verify(revokedTokenRepository, never()).save(any());
    }

    @Test
    void revoke_newToken_savesEntity() {
        long expiresAtMs = System.currentTimeMillis() + 60_000;

        tokenRevocationService.revoke("tok-123", expiresAtMs);

        ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepository).save(captor.capture());
        RevokedToken saved = captor.getValue();
        assertThat(saved.getTokenId()).isEqualTo("tok-123");
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().minusSeconds(5));
    }

    /**
     * Verifies the race-condition fix: if two concurrent callers both try to revoke the same
     * token, the second insert will hit the DB UNIQUE constraint and raise a
     * DataIntegrityViolationException. The service must catch it silently — no exception should
     * propagate to the caller.
     */
    @Test
    void revoke_duplicateInsert_dataIntegrityViolation_isSwallowedSilently() {
        doThrow(new DataIntegrityViolationException("Duplicate entry for token_id"))
                .when(revokedTokenRepository).save(any());

        // Must NOT throw — this is the race-condition path
        assertThatCode(() -> tokenRevocationService.revoke("tok-dup", System.currentTimeMillis() + 60_000))
                .doesNotThrowAnyException();

        // save() was still attempted (no pre-check suppressed it)
        verify(revokedTokenRepository).save(any());
    }

    // --- isRevoked() ---

    @Test
    void isRevoked_nullTokenId_returnsFalse() {
        assertThat(tokenRevocationService.isRevoked(null)).isFalse();
        verify(revokedTokenRepository, never()).existsByTokenId(any());
    }

    @Test
    void isRevoked_knownRevokedToken_returnsTrue() {
        when(revokedTokenRepository.existsByTokenId("tok-abc")).thenReturn(true);

        assertThat(tokenRevocationService.isRevoked("tok-abc")).isTrue();
    }

    @Test
    void isRevoked_unknownToken_returnsFalse() {
        when(revokedTokenRepository.existsByTokenId("tok-xyz")).thenReturn(false);

        assertThat(tokenRevocationService.isRevoked("tok-xyz")).isFalse();
    }
}
