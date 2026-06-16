package com.ssafy.modu.external.ai.dto.tool;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ToolChatResponse {

    private JsonNode assistantMessage;
    private String content;
    private List<ToolCall> toolCalls;

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
