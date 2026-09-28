package com.devpilot.rag;

import com.devpilot.ai.LlmClient;
import com.devpilot.dto.ChatRequest;
import com.devpilot.dto.ChatResponse;
import com.devpilot.dto.SourceReference;
import com.devpilot.entity.ChatMessage;
import com.devpilot.entity.ChatSession;
import com.devpilot.entity.CodeChunk;
import com.devpilot.embedding.EmbeddingService;
import com.devpilot.repository.ChatMessageRepository;
import com.devpilot.repository.ChatSessionRepository;
import com.devpilot.repository.CodeChunkRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final CodeChunkRepository codeChunkRepository;
    private final EmbeddingService embeddingService;
    private final LlmClient llmClient;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.rag.top-k:8}")
    private int topK;

    @Transactional
    public ChatResponse answerQuestion(Long repositoryId, ChatSession session, ChatRequest request) {
        String question = request.getQuestion();
        log.info("RAG processing question for repository {}: '{}'", repositoryId, question);

        // 1. Embed user question
        List<Double> queryEmbedding = embeddingService.embedText(question);
        String vectorString = embeddingService.vectorToString(queryEmbedding);

        // 2. Vector search + Keyword retrieval (Hybrid RAG)
        List<CodeChunk> retrievedChunks = new ArrayList<>();
        try {
            retrievedChunks = codeChunkRepository.findTopSimilarByVector(repositoryId, vectorString, topK);
        } catch (Exception e) {
            log.warn("Vector search failed or pgvector not available: {}. Falling back to keyword search.", e.getMessage());
        }

        if (retrievedChunks.isEmpty()) {
            // Keyword matching fallback
            String[] keywords = question.split("\\W+");
            Set<Long> seenIds = new HashSet<>();
            for (String kw : keywords) {
                if (kw.length() > 3) {
                    List<CodeChunk> chunks = codeChunkRepository.searchByKeyword(repositoryId, kw);
                    for (CodeChunk c : chunks) {
                        if (seenIds.add(c.getId())) {
                            retrievedChunks.add(c);
                            if (retrievedChunks.size() >= topK) break;
                        }
                    }
                }
                if (retrievedChunks.size() >= topK) break;
            }
        }

        // If still empty, grab initial repository chunks
        if (retrievedChunks.isEmpty()) {
            List<CodeChunk> allChunks = codeChunkRepository.findByRepositoryId(repositoryId);
            retrievedChunks = allChunks.stream().limit(topK).collect(Collectors.toList());
        }

        // 3. Construct Context
        StringBuilder contextBuilder = new StringBuilder();
        List<SourceReference> sources = new ArrayList<>();

        for (CodeChunk chunk : retrievedChunks) {
            contextBuilder.append("--- File: ").append(chunk.getFilePath())
                    .append(" (lines ").append(chunk.getStartLine()).append("-").append(chunk.getEndLine()).append(") ---\n")
                    .append(chunk.getContent())
                    .append("\n\n");

            sources.add(SourceReference.builder()
                    .file(chunk.getFilePath())
                    .startLine(chunk.getStartLine())
                    .endLine(chunk.getEndLine())
                    .symbolName(chunk.getSymbolName())
                    .snippet(chunk.getContent().length() > 200 ? chunk.getContent().substring(0, 200) + "..." : chunk.getContent())
                    .build());
        }

        // 4. Construct System & User Prompt
        String systemPrompt = """
            You are DevPilot, an expert AI software architect and developer code assistant.
            Your purpose is to explain codebases accurately based ONLY on the repository code provided in the context.
            
            Guidelines:
            1. Answer clearly and comprehensively, referencing exact file paths, class names, and functions.
            2. Explain the logical flow from controller/entrypoint down to data storage or external APIs.
            3. Cite relevant source files.
            4. If the provided code snippets do not contain enough information to be certain, state: "The repository contains insufficient details on this specific aspect."
            5. NEVER hallucinate or invent classes, methods, or endpoints that are not in the context.
            """;

        String userPrompt = "Repository Context Snippets:\n\n" + contextBuilder.toString() +
                "\nUser Question: " + question +
                "\n\nPlease provide a clear, technical explanation referencing the source files:";

        // 5. Query LLM
        String answer = llmClient.generateAnswer(systemPrompt, userPrompt);

        // 6. Generate flow if question asks about workflow / flow / login / request
        String codeFlow = null;
        String qLower = question.toLowerCase();
        if (qLower.contains("flow") || qLower.contains("workflow") || qLower.contains("login") || qLower.contains("auth") || qLower.contains("order")) {
            codeFlow = llmClient.generateCodeFlow(question, contextBuilder.toString());
        }

        // 7. Persist chat turn
        try {
            String sourcesJson = objectMapper.writeValueAsString(sources);

            ChatMessage userMsg = ChatMessage.builder()
                    .chatSession(session)
                    .role("user")
                    .content(question)
                    .build();
            chatMessageRepository.save(userMsg);

            ChatMessage assistantMsg = ChatMessage.builder()
                    .chatSession(session)
                    .role("assistant")
                    .content(answer)
                    .sourcesJson(sourcesJson)
                    .build();
            chatMessageRepository.save(assistantMsg);
        } catch (Exception e) {
            log.error("Failed to save chat message history", e);
        }

        return ChatResponse.builder()
                .sessionId(session.getId())
                .answer(answer)
                .sources(sources)
                .codeFlow(codeFlow)
                .build();
    }
}
