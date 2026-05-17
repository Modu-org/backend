package com.ssafy.modu.global.auth.oauth.userinfo;

import java.util.Map;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "KAKAO";
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getEmail() {
        Map<String, Object> kakaoAccount =
                (Map<String, Object>) attributes.get("kakao_account");

        if (kakaoAccount == null) {
            return null;
        }

        return (String) kakaoAccount.get("email");
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getNickname() {
        Map<String, Object> kakaoAccount =
                (Map<String, Object>) attributes.get("kakao_account");

        if (kakaoAccount == null) {
            return null;
        }

        Map<String, Object> profile =
                (Map<String, Object>) kakaoAccount.get("profile");

        if (profile == null) {
            return null;
        }

        return (String) profile.get("nickname");
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getProfileImg() {
        Map<String, Object> kakaoAccount =
                (Map<String, Object>) attributes.get("kakao_account");

        if (kakaoAccount == null) {
            return null;
        }

        Map<String, Object> profile =
                (Map<String, Object>) kakaoAccount.get("profile");

        if (profile == null) {
            return null;
        }

        return (String) profile.get("profile_image_url");
    }
}