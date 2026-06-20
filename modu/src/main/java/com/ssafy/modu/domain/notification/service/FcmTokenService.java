package com.ssafy.modu.domain.notification.service;

import com.ssafy.modu.domain.notification.entity.FcmToken;
import com.ssafy.modu.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    public void saveToken(Long userId, String token) {
        fcmTokenRepository.findByUserIdAndToken(userId, token)
                .ifPresentOrElse(
                        FcmToken::activate,
                        () -> fcmTokenRepository.save(FcmToken.create(userId, token))
                );
    }

    @Transactional(readOnly = true)
    public List<String> getActiveTokens(Long userId) {
        return fcmTokenRepository.findAllByUserIdAndActiveTrue(userId)
                .stream()
                .map(FcmToken::getToken)
                .toList();
    }

    public void deactivateToken(Long userId, String token) {
        fcmTokenRepository.findByUserIdAndToken(userId, token)
                .ifPresent(FcmToken::deactivate);
    }
}