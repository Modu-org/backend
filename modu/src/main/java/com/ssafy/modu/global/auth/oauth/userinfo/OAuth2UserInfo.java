package com.ssafy.modu.global.auth.oauth.userinfo;

public interface OAuth2UserInfo {

    String getProvider();

    String getProviderId();

    String getEmail();

    String getNickname();

    String getProfileImg();
}