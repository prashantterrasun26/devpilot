package com.devpilot.architecture;

import com.devpilot.ai.LlmClient;
import com.devpilot.dto.ArchitectureResponse;
import com.devpilot.entity.Repository;
import com.devpilot.entity.RepositoryFile;
import com.devpilot.repository.RepositoryEntityRepository;
import com.devpilot.repository.RepositoryFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchitectureService {

    private final RepositoryFileRepository fileRepository;
    private final RepositoryEntityRepository repoRepository;
    private final LlmClient llmClient;

    @Transactional
    public ArchitectureResponse analyzeArchitecture(Long repositoryId) {
        Repository repository = repoRepository.findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repositoryId));

        List<RepositoryFile> files = fileRepository.findByRepositoryIdOrderByFilePathAsc(repositoryId);

        List<String> frontendComponents = new ArrayList<>();
        List<String> backendComponents = new ArrayList<>();
        List<String> databaseComponents = new ArrayList<>();
        List<String> externalServices = new ArrayList<>();
        List<String> apiEndpoints = new ArrayList<>();
        List<String> keyModules = new ArrayList<>();

        StringBuilder fileTreeSummary = new StringBuilder();
        StringBuilder codebaseSummary = new StringBuilder();

        for (RepositoryFile f : files) {
            String path = f.getFilePath();
            fileTreeSummary.append(path).append("\n");

            String lower = path.toLowerCase();
            if (lower.contains("controller") || lower.contains("resource") || lower.contains("api/") || lower.contains("routes")) {
                backendComponents.add(path);
                // Extract REST mappings if content is present
                if (f.getContent() != null) {
                    Matcher m = Pattern.compile("@(?:Get|Post|Put|Delete|Patch)Mapping\\([\"']?([^\"'\\)]+)").matcher(f.getContent());
                    while (m.find()) {
                        apiEndpoints.add(m.group(1));
                    }
                }
            } else if (lower.endsWith(".tsx") || lower.endsWith(".jsx") || lower.contains("/components/") || lower.contains("/views/")) {
                frontendComponents.add(path);
            } else if (lower.contains("repository") || lower.contains("entity") || lower.contains("model") || lower.contains("schema.sql") || lower.contains("migration")) {
                databaseComponents.add(path);
            } else if (lower.contains("client") || lower.contains("feign") || lower.contains("webhook") || lower.contains("oauth")) {
                externalServices.add(path);
            }

            // Look for package / root folders
            String[] segments = path.split("/");
            if (segments.length > 1 && !keyModules.contains(segments[0])) {
                keyModules.add(segments[0]);
            }

            // Include readme or pom or package.json in codebase summary
            if (lower.endsWith("pom.xml") || lower.endsWith("package.json") || lower.endsWith("readme.md") || lower.endsWith("build.gradle")) {
                codebaseSummary.append("--- File: ").append(path).append(" ---\n")
                        .append(f.getContent() != null && f.getContent().length() > 1000 ? f.getContent().substring(0, 1000) : f.getContent())
                        .append("\n\n");
            }
        }

        // Generate Mermaid diagram
        String mermaid = llmClient.generateArchitectureMermaid(codebaseSummary.toString(), fileTreeSummary.toString());
        String summary = "Detected " + frontendComponents.size() + " frontend modules, " +
                backendComponents.size() + " API/controller modules, and " +
                databaseComponents.size() + " persistence entities in repository " + repository.getName() + ".";

        repository.setArchitectureMermaid(mermaid);
        repository.setArchitectureSummary(summary);
        repoRepository.save(repository);

        return ArchitectureResponse.builder()
                .mermaidDiagram(mermaid)
                .summary(summary)
                .frontendComponents(frontendComponents.stream().limit(25).collect(Collectors.toList()))
                .backendComponents(backendComponents.stream().limit(25).collect(Collectors.toList()))
                .databaseComponents(databaseComponents.stream().limit(25).collect(Collectors.toList()))
                .externalServices(externalServices.stream().limit(25).collect(Collectors.toList()))
                .apiEndpoints(apiEndpoints.stream().distinct().limit(25).collect(Collectors.toList()))
                .keyModules(keyModules)
                .build();
    }
}
