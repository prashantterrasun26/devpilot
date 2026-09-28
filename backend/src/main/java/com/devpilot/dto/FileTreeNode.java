package com.devpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileTreeNode {
    private String name;
    private String path;
    private String type; // "file" or "dir"
    private Long size;
    private List<FileTreeNode> children;
}
