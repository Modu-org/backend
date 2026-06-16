package com.ssafy.modu.domain.aicommand.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionContext;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionResult;
import com.ssafy.modu.domain.aicommand.tool.core.enums.AiCommandScope;

import java.util.Map;
import java.util.Set;

public interface ScheduleToolHandler {

    String getName();

    Set<AiCommandScope> getScopes();

    Map<String, Object> getDefinition();

    ToolExecutionResult execute(ToolExecutionContext context, JsonNode arguments);
}
