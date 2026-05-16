package com.ssafy.modu.domain.user.service;

import com.ssafy.modu.domain.user.dto.UserMeResponse;
import com.ssafy.modu.domain.user.dto.UserUpdateRequest;
import com.ssafy.modu.domain.user.dto.UserUpdateResponse;
import com.ssafy.modu.domain.user.entity.User;
import com.ssafy.modu.domain.user.entity.UserDetail;
import com.ssafy.modu.domain.user.repository.UserRepository;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserMeResponse getMe(Long userId) {
        User user = findActiveUser(userId);
        UserDetail userDetail = user.getUserDetail();

        return new UserMeResponse(
                user.getId(),
                user.getUserName(),
                user.getNickname(),
                user.getProfileImg(),
                userDetail.getPhysical(),
                userDetail.getInfantFamily(),
                userDetail.getVisual(),
                userDetail.getHearing()
        );
    }

    @Transactional
    public UserUpdateResponse updateMe(Long userId, UserUpdateRequest request) {
        User user = findActiveUser(userId);
        UserDetail userDetail = user.getUserDetail();

        user.updateProfile(
                request.nickname(),
                request.profileImg()
        );

        userDetail.updateDetail(
                defaultFalse(request.physical()),
                defaultFalse(request.infantFamily()),
                defaultFalse(request.visual()),
                defaultFalse(request.hearing())
        );

        return new UserUpdateResponse(
                user.getId(),
                user.getNickname()
        );
    }

    private User findActiveUser(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Boolean defaultFalse(Boolean value) {
        return value != null && value;
    }
}