package com.ssafy.modu.global.auth.oauth;

import com.ssafy.modu.domain.user.entity.SocialAccount;
import com.ssafy.modu.domain.user.entity.User;
import com.ssafy.modu.domain.user.entity.UserDetail;
import com.ssafy.modu.domain.user.repository.SocialAccountRepository;
import com.ssafy.modu.domain.user.repository.UserRepository;
import com.ssafy.modu.global.auth.oauth.userinfo.OAuth2UserInfo;
import com.ssafy.modu.global.auth.oauth.userinfo.OAuth2UserInfoFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest
                .getClientRegistration()
                .getRegistrationId();

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId,
                oAuth2User.getAttributes()
        );

        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderId(
                        userInfo.getProvider(),
                        userInfo.getProviderId()
                )
                .map(account -> {
                    account.updateProfile(
                            userInfo.getEmail(),
                            userInfo.getNickname(),
                            userInfo.getProfileImg()
                    );
                    return account;
                })
                .orElseGet(() -> createSocialAccount(userInfo));

        User user = socialAccount.getUser();

        return new CustomOAuth2UserPrincipal(
                user.getId(),
                user.getUserName(),
                oAuth2User.getAttributes()
        );
    }

    private SocialAccount createSocialAccount(OAuth2UserInfo userInfo) {
        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        String userName = generateUserName(userInfo);
        String nickname = generateNickname(userInfo);

        User user = User.createSocialUser(
                userName,
                randomPassword,
                nickname,
                userInfo.getProfileImg()
        );

        UserDetail userDetail = UserDetail.builder()
                .physical(false)
                .infantFamily(false)
                .visual(false)
                .hearing(false)
                .build();

        user.setUserDetail(userDetail);

        User savedUser = userRepository.save(user);

        SocialAccount socialAccount = new SocialAccount(
                savedUser,
                userInfo.getProvider(),
                userInfo.getProviderId(),
                userInfo.getEmail(),
                userInfo.getNickname(),
                userInfo.getProfileImg()
        );

        return socialAccountRepository.save(socialAccount);
    }

    private String generateUserName(OAuth2UserInfo userInfo) {
        String provider = userInfo.getProvider().toLowerCase();
        String providerId = userInfo.getProviderId();

        return provider + "_" + providerId;
    }

    private String generateNickname(OAuth2UserInfo userInfo) {
        String rawNickname = userInfo.getNickname();

        String baseNickname;

        if (rawNickname == null || rawNickname.isBlank()) {
            baseNickname = userInfo.getProvider().toLowerCase() + "_user";
        } else {
            baseNickname = rawNickname.trim();
        }

        baseNickname = limitLength(baseNickname, 20);

        if (!userRepository.existsByNicknameAndIsDeletedFalse(baseNickname)) {
            return baseNickname;
        }

        for (int i = 0; i < 10; i++) {
            String suffix = "_" + UUID.randomUUID().toString().substring(0, 4);
            String candidate = limitLength(baseNickname, 20 - suffix.length()) + suffix;

            if (!userRepository.existsByNicknameAndIsDeletedFalse(candidate)) {
                return candidate;
            }
        }

        String fallback = userInfo.getProvider().toLowerCase()
                + "_user_"
                + UUID.randomUUID().toString().substring(0, 8);

        return limitLength(fallback, 20);
    }

    private String limitLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }


}