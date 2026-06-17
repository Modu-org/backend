package com.ssafy.modu.domain.aicommand.dto.response;

import com.ssafy.modu.domain.aicommand.dto.enums.VoiceCommandResultType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VoiceCommandResponse {

    private VoiceCommandResultType resultType;
    private String message;
    private Object data;

    public static VoiceCommandResponse of(
            VoiceCommandResultType resultType,
            String message,
            Object data
    ) {
        return new VoiceCommandResponse(resultType, message, data);
    }
}
