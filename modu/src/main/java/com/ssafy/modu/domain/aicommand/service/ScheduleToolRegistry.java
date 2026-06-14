package com.ssafy.modu.domain.aicommand.service;

import com.ssafy.modu.domain.aicommand.tool.ScheduleToolHandler;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ScheduleToolRegistry {

    private final List<ScheduleToolHandler> handlers;

    public ScheduleToolHandler getHandler(String toolName) {
        return handlers.stream()
                .filter(handler -> handler.getName().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNSUPPORTED_AI_TOOL));
    }

    public List<Map<String, Object>> getDefinitions() {
        return handlers.stream()
                .map(ScheduleToolHandler::getDefinition)
                .toList();
    }
}
