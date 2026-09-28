package com.devpilot.service;

import com.devpilot.dto.RepositoryDto;
import com.devpilot.dto.RepositoryStatusDto;
import com.devpilot.entity.AnalysisStatus;
import com.devpilot.entity.Repository;
import com.devpilot.entity.User;
import com.devpilot.exception.DevPilotException;
import com.devpilot.github.GitHubRepositoryResponse;
import com.devpilot.github.GitHubService;
import com.devpilot.repository.RepositoryEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryService {

    private final RepositoryEntityRepository repoRepository;
    private final GitHubService gitHubService;
    private final RepositoryAnalysisService analysisService;

    @Transactional(readOnly = true)
    public List<RepositoryDto> getUserRepositories(User user) {
        return repoRepository.findByUserIdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<RepositoryDto> syncGitHubRepositories(User user) {
        log.info("Syncing GitHub repositories for user {}", user.getUsername());
        List<GitHubRepositoryResponse> ghRepos = gitHubService.getUserRepositories(user.getAccessToken(), 1, 100);

        for (GitHubRepositoryResponse gh : ghRepos) {
            repoRepository.findByUserIdAndGithubRepoId(user.getId(), gh.getId())
                    .map(existing -> {
                        existing.setName(gh.getName());
                        existing.setFullName(gh.getFullName());
                        existing.setDescription(gh.getDescription());
                        existing.setHtmlUrl(gh.getHtmlUrl());
                        existing.setDefaultBranch(gh.getDefaultBranch());
                        existing.setIsPrivate(gh.getIsPrivate());
                        existing.setLanguage(gh.getLanguage());
                        return repoRepository.save(existing);
                    })
                    .orElseGet(() -> repoRepository.save(Repository.builder()
                            .user(user)
                            .githubRepoId(gh.getId())
                            .name(gh.getName())
                            .fullName(gh.getFullName())
                            .ownerLogin(gh.getOwner() != null ? gh.getOwner().getLogin() : user.getUsername())
                            .description(gh.getDescription())
                            .htmlUrl(gh.getHtmlUrl())
                            .defaultBranch(gh.getDefaultBranch() != null ? gh.getDefaultBranch() : "main")
                            .isPrivate(gh.getIsPrivate())
                            .language(gh.getLanguage())
                            .status(AnalysisStatus.NOT_ANALYZED)
                            .build()));
        }

        return getUserRepositories(user);
    }

    @Transactional(readOnly = true)
    public RepositoryDto getRepositoryById(Long id, User user) {
        Repository repo = repoRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> DevPilotException.notFound("Repository not found with ID: " + id));
        return toDto(repo);
    }

    @Transactional
    public RepositoryStatusDto triggerAnalysis(Long id, User user) {
        Repository repo = repoRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> DevPilotException.notFound("Repository not found with ID: " + id));

        repo.setStatus(AnalysisStatus.QUEUED);
        repoRepository.save(repo);

        analysisService.startAnalysisAsync(repo.getId(), user.getAccessToken());

        return RepositoryStatusDto.builder()
                .id(repo.getId())
                .status(AnalysisStatus.QUEUED)
                .filesScanned(repo.getFilesCount())
                .chunksGenerated(repo.getChunksCount())
                .errorMessage(null)
                .build();
    }

    @Transactional(readOnly = true)
    public RepositoryStatusDto getAnalysisStatus(Long id, User user) {
        Repository repo = repoRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> DevPilotException.notFound("Repository not found with ID: " + id));

        return RepositoryStatusDto.builder()
                .id(repo.getId())
                .status(repo.getStatus())
                .filesScanned(repo.getFilesCount())
                .chunksGenerated(repo.getChunksCount())
                .errorMessage(repo.getErrorMessage())
                .lastCommitSha(repo.getLastCommitSha())
                .build();
    }

    public RepositoryDto toDto(Repository entity) {
        return RepositoryDto.builder()
                .id(entity.getId())
                .githubRepoId(entity.getGithubRepoId())
                .name(entity.getName())
                .fullName(entity.getFullName())
                .ownerLogin(entity.getOwnerLogin())
                .description(entity.getDescription())
                .htmlUrl(entity.getHtmlUrl())
                .defaultBranch(entity.getDefaultBranch())
                .isPrivate(entity.getIsPrivate())
                .language(entity.getLanguage())
                .status(entity.getStatus())
                .filesCount(entity.getFilesCount())
                .chunksCount(entity.getChunksCount())
                .errorMessage(entity.getErrorMessage())
                .lastCommitSha(entity.getLastCommitSha())
                .architectureMermaid(entity.getArchitectureMermaid())
                .architectureSummary(entity.getArchitectureSummary())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
