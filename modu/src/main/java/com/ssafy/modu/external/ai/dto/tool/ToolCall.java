package com.ssafy.modu.external.ai.dto.tool;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ToolCall {

    private String id;
    private String name;
    private JsonNode arguments;
}
