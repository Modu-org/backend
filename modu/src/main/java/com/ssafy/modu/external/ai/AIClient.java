package com.ssafy.modu.external.ai;

public interface AIClient {

    String generateContent(String prompt);

    default String generateContent(String developerPrompt, String userPrompt) {
        return generateContent(developerPrompt + "\n\n" + userPrompt);
    }
}