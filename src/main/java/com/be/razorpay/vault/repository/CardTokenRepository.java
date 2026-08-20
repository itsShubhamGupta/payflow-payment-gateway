package com.be.razorpay.vault.repository;

import com.be.razorpay.vault.entity.CardToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository

public interface CardTokenRepository extends JpaRepository<CardToken, UUID> {
    Optional<CardToken> findByTokenAndRevokedAtIsNull(String token);
}