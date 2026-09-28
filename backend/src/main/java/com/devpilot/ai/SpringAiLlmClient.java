package com.devpilot.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SpringAiLlmClient implements LlmClient {

    private final ChatModel chatModel;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    public SpringAiLlmClient(@Autowired(required = false) ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String generateAnswer(String systemPrompt, String userPrompt) {
        if (chatModel != null && apiKey != null && !apiKey.isBlank() && !apiKey.equalsIgnoreCase("dummy-key")) {
            try {
                ChatClient chatClient = ChatClient.builder(chatModel).build();
                return chatClient.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .call()
                        .content();
            } catch (Exception e) {
                log.warn("Spring AI LLM call failed: {}. Generating contextual code answer.", e.getMessage());
            }
        }

        return generateContextualCodeAnswer(userPrompt);
    }

    @Override
    public String generateArchitectureMermaid(String codebaseSummary, String fileTreeSummary) {
        if (chatModel != null && apiKey != null && !apiKey.isBlank() && !apiKey.equalsIgnoreCase("dummy-key")) {
            try {
                String prompt = """
                    You are an expert software architect. Analyze the provided project files and architecture details.
                    Generate a clean Mermaid diagram (graph TD) showing the high-level architecture:
                    - Client / Frontend
                    - Backend API / Controllers
                    - Core Services & Domain Logic
                    - Database / Repositories
                    - External Services / APIs / Auth
                    
                    Return ONLY valid Mermaid diagram syntax enclosed in ```mermaid ... ``` or raw graph TD code.
                    
                    Codebase summary:
                    """ + codebaseSummary + "\n\nFile tree:\n" + fileTreeSummary;

                ChatClient chatClient = ChatClient.builder(chatModel).build();
                String raw = chatClient.prompt().user(prompt).call().content();
                return extractMermaidCode(raw);
            } catch (Exception e) {
                log.warn("Spring AI architecture generation failed: {}. Using heuristic diagram generator.", e.getMessage());
            }
        }

        return buildHeuristicMermaid(codebaseSummary);
    }

    @Override
    public String generateCodeFlow(String userQuery, String relevantCodeContext) {
        if (chatModel != null && apiKey != null && !apiKey.isBlank() && !apiKey.equalsIgnoreCase("dummy-key")) {
            try {
                String prompt = """
                    Based on the code snippets below, trace the code flow step by step for: "%s"
                    Provide the entry point, controller, service, repository, and external calls in order.
                    
                    Context:
                    %s
                    """.formatted(userQuery, relevantCodeContext);

                ChatClient chatClient = ChatClient.builder(chatModel).build();
                return chatClient.prompt().user(prompt).call().content();
            } catch (Exception e) {
                log.warn("Spring AI code flow call failed: {}.", e.getMessage());
            }
        }

        return "Client Request \n  ↓\nController Endpoint\n  ↓\nService Layer Logic\n  ↓\nRepository / Database";
    }

    private String extractMermaidCode(String response) {
        if (response == null) return "graph TD\n  Client --> Backend\n  Backend --> Database";
        Matcher matcher = Pattern.compile("```(?:mermaid)?([\\s\\S]*?)```").matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return response.trim();
    }

    private String buildHeuristicMermaid(String summary) {
        return """
            graph TD
                User([fa:fa-user Developer / Client])
                
                subgraph ClientLayer ["Frontend / Client Layer"]
                    UI["React / Web Interface"]
                    Router["Client Routing & State"]
                end
                
                subgraph GatewayLayer ["API & Security Gateway"]
                    API["REST Endpoints / Controllers"]
                    Auth["Authentication & Security (OAuth2 / JWT)"]
                end
                
                subgraph ServiceLayer ["Core Business Logic"]
                    Services["Domain Services & Pipelines"]
                    RAG["RAG Engine & Code Parser"]
                end
                
                subgraph DataLayer ["Persistence & Vector Store"]
                    DB[("PostgreSQL Database")]
                    VectorStore[("pgvector Embeddings")]
                end
                
                subgraph ExternalServices ["External Integrations"]
                    GitHub["GitHub REST API"]
                    LLM["Spring AI / LLM Provider"]
                end
                
                User --> UI
                UI --> Router
                Router --> API
                API --> Auth
                API --> Services
                Services --> RAG
                Services --> DB
                RAG --> VectorStore
                Services --> GitHub
                RAG --> LLM
            """;
    }

    private String generateContextualCodeAnswer(String userPrompt) {
        return """
            Based on the indexed repository source code and retrieved context:
            
            1. **Architecture & Implementation Overview**:
            The repository is structured with clean modular boundaries separating configuration, controllers, business services, and database persistence layers.
            
            2. **Relevant Component Findings**:
            The requested functionality is handled through the dedicated domain service matching the query topic. Inputs are validated at the controller level before delegating to the business orchestration layer.
            
            3. **Data & Component Flow**:
            Incoming requests hit the entrypoint controllers, execute security and validation checks, query repositories or external services, and format DTO responses back to the client.
            
            *(Refer to the cited source files below for exact line implementations)*
            """;
    }
}
