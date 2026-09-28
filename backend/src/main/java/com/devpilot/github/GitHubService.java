package com.devpilot.github;

import com.devpilot.exception.DevPilotException;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    private final RestClient restClient;
    private static final String GITHUB_API_BASE = "https://api.github.com";

    public Map<String, Object> getAuthenticatedUser(String accessToken) {
        log.info("Fetching authenticated GitHub user profile");
        try {
            return restClient.get()
                    .uri(GITHUB_API_BASE + "/user")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw DevPilotException.gitHubError("GitHub user lookup failed with status: " + resp.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch authenticated user from GitHub", e);
            throw DevPilotException.gitHubError("Could not retrieve GitHub user profile: " + e.getMessage());
        }
    }

    public List<GitHubRepositoryResponse> getUserRepositories(String accessToken, int page, int perPage) {
        log.info("Fetching user repositories from GitHub (page: {}, perPage: {})", page, perPage);
        try {
            return restClient.get()
                    .uri(GITHUB_API_BASE + "/user/repos?sort=updated&per_page=" + perPage + "&page=" + page)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw DevPilotException.gitHubError("GitHub get repos failed with status: " + resp.getStatusCode());
                    })
                    .body(new ParameterizedTypeReference<List<GitHubRepositoryResponse>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch user repositories from GitHub", e);
            return Collections.emptyList();
        }
    }

    public GitHubRepositoryResponse getRepository(String accessToken, String owner, String repo) {
        log.info("Fetching repo metadata for {}/{}", owner, repo);
        try {
            return restClient.get()
                    .uri(GITHUB_API_BASE + "/repos/" + owner + "/" + repo)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw DevPilotException.gitHubError("GitHub get repo failed with status: " + resp.getStatusCode());
                    })
                    .body(GitHubRepositoryResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch repository {}/{}", owner, repo, e);
            throw DevPilotException.gitHubError("Repository not found or access denied: " + e.getMessage());
        }
    }

    public GitHubTreeResponse getRepositoryTree(String accessToken, String owner, String repo, String treeSha) {
        log.info("Fetching repository tree for {}/{} (ref: {})", owner, repo, treeSha);
        try {
            return restClient.get()
                    .uri(GITHUB_API_BASE + "/repos/" + owner + "/" + repo + "/git/trees/" + treeSha + "?recursive=1")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw DevPilotException.gitHubError("GitHub tree fetch failed: " + resp.getStatusCode());
                    })
                    .body(GitHubTreeResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch repo tree for {}/{}", owner, repo, e);
            throw DevPilotException.gitHubError("Failed to fetch repository tree: " + e.getMessage());
        }
    }

    @Data
    public static class GitHubContentResponse {
        private String content;
        private String encoding;
        private Long size;
        private String name;
        private String path;
        private String sha;
    }

    public String getFileContent(String accessToken, String owner, String repo, String filePath, String ref) {
        log.debug("Fetching file content for {}/{}: {}", owner, repo, filePath);
        try {
            GitHubContentResponse response = restClient.get()
                    .uri(GITHUB_API_BASE + "/repos/" + owner + "/" + repo + "/contents/" + filePath + (ref != null ? "?ref=" + ref : ""))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw DevPilotException.gitHubError("Failed to fetch file content: " + resp.getStatusCode());
                    })
                    .body(GitHubContentResponse.class);

            if (response != null && response.getContent() != null) {
                if ("base64".equalsIgnoreCase(response.getEncoding())) {
                    String cleanBase64 = response.getContent().replaceAll("\\s+", "");
                    byte[] decoded = Base64.getDecoder().decode(cleanBase64);
                    return new String(decoded, StandardCharsets.UTF_8);
                }
                return response.getContent();
            }
            return "";
        } catch (Exception e) {
            log.warn("Could not retrieve file content for {} in {}/{}: {}", filePath, owner, repo, e.getMessage());
            return "";
        }
    }

    public List<Map<String, Object>> getBranches(String accessToken, String owner, String repo) {
        try {
            return restClient.get()
                    .uri(GITHUB_API_BASE + "/repos/" + owner + "/" + repo + "/branches")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("Failed to get branches for {}/{}", owner, repo, e);
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getCommits(String accessToken, String owner, String repo, int limit) {
        try {
            return restClient.get()
                    .uri(GITHUB_API_BASE + "/repos/" + owner + "/" + repo + "/commits?per_page=" + limit)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("Failed to get commits for {}/{}", owner, repo, e);
            return Collections.emptyList();
        }
    }
}
