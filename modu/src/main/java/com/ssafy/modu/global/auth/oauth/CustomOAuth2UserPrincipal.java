package com.ssafy.modu.global.auth.oauth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class CustomOAuth2UserPrincipal implements OAuth2User {

    private final Long userId;
    private final String userName;
    private final Map<String, Object> attributes;

    public CustomOAuth2UserPrincipal(
            Long userId,
            String userName,
            Map<String, Object> attributes
    ) {
        this.userId = userId;
        this.userName = userName;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}