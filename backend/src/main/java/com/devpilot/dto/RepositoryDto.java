package com.devpilot.dto;

import com.devpilot.entity.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryDto {
    private Long id;
    private Long githubRepoId;
    private String name;
    private String fullName;
    private String ownerLogin;
    private String description;
    private String htmlUrl;
    private String defaultBranch;
    private Boolean isPrivate;
    private String language;
    private AnalysisStatus status;
    private Integer filesCount;
    private Integer chunksCount;
    private String errorMessage;
    private String lastCommitSha;
    private String architectureMermaid;
    private String architectureSummary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
