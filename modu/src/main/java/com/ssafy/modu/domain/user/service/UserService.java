package com.ssafy.modu.domain.user.service;

import com.ssafy.modu.domain.user.dto.UserMeResponse;
import com.ssafy.modu.domain.user.dto.UserUpdateRequest;
import com.ssafy.modu.domain.user.dto.UserUpdateResponse;
import com.ssafy.modu.domain.user.entity.User;
import com.ssafy.modu.domain.user.entity.UserDetail;
import com.ssafy.modu.domain.user.enums.UiMode;
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
                userDetail.getAgeGroupCode(),
                userDetail.getTripStyleCode(),
                userDetail.getUsesWheelchair(),
                userDetail.getHasStroller(),
                userDetail.getUsesWalkingAid(),
                userDetail.getHasServiceDog(),
                userDetail.getNeedsVisualAssistance(),
                userDetail.getUiMode()
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
                request.ageGroupCode(),
                request.tripStyleCode(),
                defaultFalse(request.usesWheelchair()),
                defaultFalse(request.hasStroller()),
                defaultFalse(request.usesWalkingAid()),
                defaultFalse(request.hasServiceDog()),
                defaultFalse(request.needsVisualAssistance()),
                request.uiMode() == null ? UiMode.STANDARD : request.uiMode()
        );

        return new UserUpdateResponse(
                user.getId(),
                user.getNickname(),
                userDetail.getUiMode()
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