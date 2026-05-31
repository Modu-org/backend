package com.ssafy.modu.domain.node.controller;

import com.ssafy.modu.domain.node.dto.request.NodeArrangementRequest;
import com.ssafy.modu.domain.node.dto.request.NodeCreateRequest;
import com.ssafy.modu.domain.node.dto.response.NodeDetailResponse;
import com.ssafy.modu.domain.node.dto.response.NodeResponse;
import com.ssafy.modu.domain.node.service.NodeService;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleDetailResponse;
import com.ssafy.modu.global.auth.security.CustomUserDetails;
import com.ssafy.modu.global.common.ApiResponse;
import com.ssafy.modu.global.common.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules/{scheduleId}/nodes")
public class NodeController {

    private final NodeService nodeService;

    // 스케줄에 노드 추가
    @PostMapping
    public ResponseEntity<ApiResponse<NodeResponse>> addNode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long scheduleId,
            @Valid @RequestBody NodeCreateRequest request
    ) {
        NodeResponse data = nodeService.addNode(userDetails.getUserId(), scheduleId, request);
        return ResponseEntity.status(SuccessCode.CREATED.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.CREATED, "노드가 추가되었습니다.", data));
    }
    // 노드 상세 정보 보기
    @GetMapping("/{nodeId}")
    public ResponseEntity<ApiResponse<NodeDetailResponse>> getNodeDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long scheduleId,
            @PathVariable Long nodeId
    ) {
        NodeDetailResponse data = nodeService.getNodeDetail(userDetails.getUserId(), scheduleId, nodeId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, "노드 상세 조회에 성공했습니다.", data));
    }

    // 노드 정보 수정
    @PutMapping("/placement")
    public ResponseEntity<ApiResponse<ScheduleDetailResponse>> updateNodeArrangement(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long scheduleId,
            @Valid @RequestBody NodeArrangementRequest request
    ) {
        ScheduleDetailResponse data =
                nodeService.updateNodeArrangement(userDetails.getUserId(), scheduleId, request);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.OK, "노드 배치가 저장되었습니다.", data)
        );
    }

    // 노드 삭제
    @DeleteMapping("/{nodeId}")
    public ResponseEntity<ApiResponse<Void>> deleteNode(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long scheduleId,
            @PathVariable Long nodeId
    ) {
        nodeService.deleteNode(userDetails.getUserId(), scheduleId, nodeId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, "노드가 삭제되었습니다."));
    }
}
