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
public class ArchitectureResponse {
    private String mermaidDiagram;
    private String summary;
    private List<String> frontendComponents;
    private List<String> backendComponents;
    private List<String> databaseComponents;
    private List<String> externalServices;
    private List<String> apiEndpoints;
    private List<String> keyModules;
}
