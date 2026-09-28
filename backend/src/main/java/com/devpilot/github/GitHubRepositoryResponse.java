package com.devpilot.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GitHubRepositoryResponse {
    private Long id;
    private String name;
    @JsonProperty("full_name")
    private String fullName;
    private String description;
    @JsonProperty("html_url")
    private String htmlUrl;
    @JsonProperty("default_branch")
    private String defaultBranch;
    @JsonProperty("private")
    private Boolean isPrivate;
    private String language;
    @JsonProperty("stargazers_count")
    private Integer stargazersCount;
    @JsonProperty("forks_count")
    private Integer forksCount;
    @JsonProperty("open_issues_count")
    private Integer openIssuesCount;
    private GitHubOwner owner;

    @Data
    public static class GitHubOwner {
        private String login;
        private Long id;
        @JsonProperty("avatar_url")
        private String avatarUrl;
    }
}
