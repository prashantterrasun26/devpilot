package com.devpilot.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GitHubTreeResponse {
    private String sha;
    private String url;
    private List<TreeItem> tree;
    private Boolean truncated;

    @Data
    public static class TreeItem {
        private String path;
        private String mode;
        private String type; // "blob" or "tree"
        private String sha;
        private Long size;
        private String url;
    }
}
