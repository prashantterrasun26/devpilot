package com.devpilot.controller;

import com.devpilot.dto.ApiResponse;
import com.devpilot.dto.FileTreeNode;
import com.devpilot.dto.RepositoryDto;
import com.devpilot.dto.RepositoryStatusDto;
import com.devpilot.entity.RepositoryFile;
import com.devpilot.entity.User;
import com.devpilot.exception.DevPilotException;
import com.devpilot.repository.RepositoryFileRepository;
import com.devpilot.security.UserPrincipal;
import com.devpilot.service.RepositoryAnalysisService;
import com.devpilot.service.RepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryService repositoryService;
    private final RepositoryAnalysisService analysisService;
    private final RepositoryFileRepository fileRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RepositoryDto>>> listRepositories(@AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        List<RepositoryDto> repos = repositoryService.getUserRepositories(user);
        return ResponseEntity.ok(ApiResponse.ok(repos));
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<List<RepositoryDto>>> syncRepositories(@AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        List<RepositoryDto> repos = repositoryService.syncGitHubRepositories(user);
        return ResponseEntity.ok(ApiResponse.ok("Repositories synced from GitHub", repos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RepositoryDto>> getRepository(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        RepositoryDto repo = repositoryService.getRepositoryById(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok(repo));
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<ApiResponse<RepositoryStatusDto>> analyzeRepository(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        RepositoryStatusDto status = repositoryService.triggerAnalysis(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok("Analysis pipeline initiated", status));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RepositoryStatusDto>> getStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        RepositoryStatusDto status = repositoryService.getAnalysisStatus(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    @GetMapping("/{id}/files")
    public ResponseEntity<ApiResponse<List<FileTreeNode>>> getFileTree(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Ensure user has access
        repositoryService.getRepositoryById(id, principal.getUser());
        List<FileTreeNode> tree = analysisService.buildFileTree(id);
        return ResponseEntity.ok(ApiResponse.ok(tree));
    }

    @GetMapping("/{id}/file-content")
    public ResponseEntity<ApiResponse<String>> getFileContent(
            @PathVariable Long id,
            @RequestParam String path,
            @AuthenticationPrincipal UserPrincipal principal) {
        repositoryService.getRepositoryById(id, principal.getUser());
        RepositoryFile file = fileRepository.findByRepositoryIdAndFilePath(id, path)
                .orElseThrow(() -> DevPilotException.notFound("File not found: " + path));

        return ResponseEntity.ok(ApiResponse.ok(file.getContent()));
    }
}
