package com.ssafy.modu.global.auth.security;

import com.ssafy.modu.domain.user.entity.User;
import com.ssafy.modu.domain.user.repository.UserRepository;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public CustomUserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUserNameAndIsDeletedFalse(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        return new CustomUserDetails(user);
    }
}