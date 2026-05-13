package com.ssafy.modu.global.auth.security;

import com.ssafy.modu.domain.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String userName;
    private final String password;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.userName = user.getUserName();
        this.password = user.getPassword();
    }

    public CustomUserDetails(Long userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.password = "";
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public String getPassword() {
        return password;
    }
}