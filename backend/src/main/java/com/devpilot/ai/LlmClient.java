package com.devpilot.ai;

import java.util.Map;

public interface LlmClient {

    String generateAnswer(String systemPrompt, String userPrompt);

    String generateArchitectureMermaid(String codebaseSummary, String fileTreeSummary);

    String generateCodeFlow(String userQuery, String relevantCodeContext);
}
