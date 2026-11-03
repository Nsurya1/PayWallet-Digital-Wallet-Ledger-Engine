package com.paywallet.auth.repository;

import com.paywallet.auth.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findFirstByUserIdAndTokenAndUsedFalseOrderByCreatedAtDesc(UUID userId, String token);

    void deleteAllByUserId(UUID userId);
}
