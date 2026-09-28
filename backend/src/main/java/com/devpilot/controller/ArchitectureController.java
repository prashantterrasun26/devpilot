package com.devpilot.controller;

import com.devpilot.architecture.ArchitectureService;
import com.devpilot.dto.ApiResponse;
import com.devpilot.dto.ArchitectureResponse;
import com.devpilot.security.UserPrincipal;
import com.devpilot.service.RepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/repositories/{id}/architecture")
@RequiredArgsConstructor
public class ArchitectureController {

    private final ArchitectureService architectureService;
    private final RepositoryService repositoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<ArchitectureResponse>> generateArchitecture(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Verify repository ownership
        repositoryService.getRepositoryById(id, principal.getUser());

        ArchitectureResponse response = architectureService.analyzeArchitecture(id);
        return ResponseEntity.ok(ApiResponse.ok("Architecture generated", response));
    }
}
