package com.ssafy.modu.domain.aicommand.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionContext;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionResult;
import com.ssafy.modu.domain.aicommand.tool.ScheduleToolHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduleToolExecutor {

    private final ScheduleToolRegistry toolRegistry;

    public ToolExecutionResult execute(
            ToolExecutionContext context,
            String toolName,
            JsonNode arguments
    ) {
        ScheduleToolHandler handler = toolRegistry.getHandler(
                toolName,
                context.getScope()
        );

        return handler.execute(context, arguments);
    }
}