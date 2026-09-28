package com.devpilot.controller;

import com.devpilot.dto.ApiResponse;
import com.devpilot.dto.ChatRequest;
import com.devpilot.dto.ChatResponse;
import com.devpilot.entity.ChatSession;
import com.devpilot.entity.Repository;
import com.devpilot.entity.User;
import com.devpilot.exception.DevPilotException;
import com.devpilot.rag.RagService;
import com.devpilot.repository.ChatSessionRepository;
import com.devpilot.repository.RepositoryEntityRepository;
import com.devpilot.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/repositories/{id}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;
    private final RepositoryEntityRepository repoRepository;
    private final ChatSessionRepository chatSessionRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> askQuestion(
            @PathVariable Long id,
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = principal.getUser();
        Repository repository = repoRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> DevPilotException.notFound("Repository not found with ID: " + id));

        ChatSession session;
        if (request.getSessionId() != null) {
            session = chatSessionRepository.findByIdAndUserId(request.getSessionId(), user.getId())
                    .orElseGet(() -> createSession(user, repository, request.getQuestion()));
        } else {
            session = createSession(user, repository, request.getQuestion());
        }

        ChatResponse response = ragService.answerQuestion(id, session, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private ChatSession createSession(User user, Repository repo, String title) {
        String shortenedTitle = (title != null && title.length() > 40) ? title.substring(0, 40) + "..." : title;
        ChatSession session = ChatSession.builder()
                .user(user)
                .repository(repo)
                .title(shortenedTitle)
                .build();
        return chatSessionRepository.save(session);
    }
}
