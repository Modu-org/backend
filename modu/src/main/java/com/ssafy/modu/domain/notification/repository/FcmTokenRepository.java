package com.ssafy.modu.domain.notification.repository;

import com.ssafy.modu.domain.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByUserIdAndToken(Long userId, String token);

    List<FcmToken> findAllByUserIdAndActiveTrue(Long userId);
}