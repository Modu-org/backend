package com.ssafy.modu.domain.caregiver.controller;

import com.ssafy.modu.domain.caregiver.dto.request.CaregiverRelationRequest;
import com.ssafy.modu.domain.caregiver.dto.response.CaregiverRelationResponse;
import com.ssafy.modu.domain.caregiver.service.CaregiverRelationService;
import com.ssafy.modu.global.auth.security.CustomUserDetails;
import com.ssafy.modu.global.common.ApiResponse;
import com.ssafy.modu.global.common.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/caregivers")
public class CaregiverRelationController {

    private final CaregiverRelationService caregiverRelationService;

    @PostMapping
    public ResponseEntity<ApiResponse<CaregiverRelationResponse>> addCaregiver(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CaregiverRelationRequest request
    ) {
        CaregiverRelationResponse data = caregiverRelationService.addCaregiver(
                userDetails.getUserId(),
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        "보호자가 등록되었습니다.",
                        data
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CaregiverRelationResponse>>> getMyCaregivers(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<CaregiverRelationResponse> data =
                caregiverRelationService.getMyCaregivers(userDetails.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        "보호자 목록 조회에 성공했습니다.",
                        data
                )
        );
    }

    @DeleteMapping("/{caregiverId}")
    public ResponseEntity<ApiResponse<Void>> removeCaregiver(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long caregiverId
    ) {
        caregiverRelationService.removeCaregiver(
                userDetails.getUserId(),
                caregiverId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        "보호자 등록이 해제되었습니다."
                )
        );
    }
}