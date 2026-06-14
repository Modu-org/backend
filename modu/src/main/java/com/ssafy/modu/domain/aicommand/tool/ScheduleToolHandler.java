package com.ssafy.modu.domain.aicommand.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionContext;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionResult;

import java.util.Map;

public interface ScheduleToolHandler {

    String getName();

    Map<String, Object> getDefinition();

    ToolExecutionResult execute(ToolExecutionContext context, JsonNode arguments);
}
