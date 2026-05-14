package com.ssafy.modu.domain.user.controller;

import com.ssafy.modu.domain.user.dto.UserMeResponse;
import com.ssafy.modu.domain.user.dto.UserUpdateRequest;
import com.ssafy.modu.domain.user.dto.UserUpdateResponse;
import com.ssafy.modu.domain.user.service.UserService;
import com.ssafy.modu.global.auth.security.CustomUserDetails;
import com.ssafy.modu.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> getMe(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserMeResponse response = userService.getMe(userDetails.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success("내 정보 조회에 성공했습니다.", response)
        );
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserUpdateResponse>> updateMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        UserUpdateResponse response = userService.updateMe(userDetails.getUserId(), request);

        return ResponseEntity.ok(
                ApiResponse.success("내 정보가 수정되었습니다.", response)
        );
    }
}