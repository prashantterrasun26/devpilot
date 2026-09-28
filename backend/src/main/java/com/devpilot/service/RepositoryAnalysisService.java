package com.devpilot.service;

import com.devpilot.dto.FileTreeNode;
import com.devpilot.embedding.EmbeddingService;
import com.devpilot.entity.AnalysisStatus;
import com.devpilot.entity.CodeChunk;
import com.devpilot.entity.Repository;
import com.devpilot.entity.RepositoryFile;
import com.devpilot.github.GitHubRepositoryResponse;
import com.devpilot.github.GitHubService;
import com.devpilot.github.GitHubTreeResponse;
import com.devpilot.parser.CodeParserService;
import com.devpilot.parser.RawCodeChunk;
import com.devpilot.repository.CodeChunkRepository;
import com.devpilot.repository.RepositoryEntityRepository;
import com.devpilot.repository.RepositoryFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryAnalysisService {

    private final GitHubService gitHubService;
    private final RepositoryEntityRepository repoRepository;
    private final RepositoryFileRepository fileRepository;
    private final CodeChunkRepository chunkRepository;
    private final CodeParserService parserService;
    private final EmbeddingService embeddingService;

    @Value("${app.indexing.max-file-size-bytes:500000}")
    private long maxFileSizeBytes;

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git", "node_modules", "target", "build", "dist", ".idea", ".vscode", "vendor", "__pycache__", ".gradle"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".java", ".js", ".jsx", ".ts", ".tsx", ".py", ".go", ".rs", ".cs", ".cpp", ".c", ".h",
            ".kt", ".sql", ".html", ".css", ".yaml", ".yml", ".json", ".md", ".xml", ".gradle", ".properties"
    );

    @Async
    @Transactional
    public void startAnalysisAsync(Long repositoryId, String userAccessToken) {
        log.info("Starting asynchronous analysis for repository ID: {}", repositoryId);
        Optional<Repository> repoOpt = repoRepository.findById(repositoryId);
        if (repoOpt.isEmpty()) {
            log.error("Repository ID {} not found for analysis", repositoryId);
            return;
        }

        Repository repository = repoOpt.get();
        repository.setStatus(AnalysisStatus.INDEXING);
        repository.setErrorMessage(null);
        repoRepository.save(repository);

        try {
            // 1. Fetch latest repo details to get default branch & HEAD commit
            GitHubRepositoryResponse ghRepo = gitHubService.getRepository(
                    userAccessToken, repository.getOwnerLogin(), repository.getName());
            String defaultBranch = ghRepo.getDefaultBranch() != null ? ghRepo.getDefaultBranch() : "main";
            repository.setDefaultBranch(defaultBranch);

            // 2. Fetch Recursive Git Tree
            GitHubTreeResponse treeResponse = gitHubService.getRepositoryTree(
                    userAccessToken, repository.getOwnerLogin(), repository.getName(), defaultBranch);

            if (treeResponse == null || treeResponse.getTree() == null) {
                throw new RuntimeException("Could not retrieve file tree from GitHub");
            }

            repository.setLastCommitSha(treeResponse.getSha());

            // 3. Clear existing files and chunks for fresh re-indexing
            chunkRepository.deleteByRepositoryId(repositoryId);
            fileRepository.deleteByRepositoryId(repositoryId);

            int filesCount = 0;
            int totalChunksCount = 0;

            List<RepositoryFile> savedFiles = new ArrayList<>();
            List<CodeChunk> pendingChunks = new ArrayList<>();

            for (GitHubTreeResponse.TreeItem item : treeResponse.getTree()) {
                if (!"blob".equals(item.getType())) {
                    continue;
                }

                String path = item.getPath();
                if (shouldIgnorePath(path)) {
                    continue;
                }

                if (item.getSize() != null && item.getSize() > maxFileSizeBytes) {
                    continue;
                }

                // 4. Download content
                String content = gitHubService.getFileContent(
                        userAccessToken, repository.getOwnerLogin(), repository.getName(), path, defaultBranch);

                if (content.isBlank()) {
                    continue;
                }

                filesCount++;
                RepositoryFile fileEntity = RepositoryFile.builder()
                        .repository(repository)
                        .filePath(path)
                        .fileSize(item.getSize() != null ? item.getSize() : (long) content.length())
                        .sha(item.getSha())
                        .language(detectLanguage(path))
                        .content(content)
                        .build();

                fileEntity = fileRepository.save(fileEntity);
                savedFiles.add(fileEntity);

                // 5. Parse and Chunk
                List<RawCodeChunk> rawChunks = parserService.parseFile(path, fileEntity.getLanguage(), content);
                for (RawCodeChunk raw : rawChunks) {
                    CodeChunk chunk = CodeChunk.builder()
                            .repository(repository)
                            .repositoryFile(fileEntity)
                            .filePath(raw.getFilePath())
                            .language(raw.getLanguage())
                            .symbolName(raw.getSymbolName())
                            .className(raw.getClassName())
                            .startLine(raw.getStartLine())
                            .endLine(raw.getEndLine())
                            .content(raw.getContent())
                            .metadata(raw.getMetadata())
                            .commitSha(treeResponse.getSha())
                            .build();

                    pendingChunks.add(chunk);
                    totalChunksCount++;
                }

                // Periodic batch embedding generation and persisting
                if (pendingChunks.size() >= 30) {
                    flushChunks(pendingChunks);
                    pendingChunks.clear();
                }
            }

            if (!pendingChunks.isEmpty()) {
                flushChunks(pendingChunks);
                pendingChunks.clear();
            }

            // 6. Mark READY
            repository.setFilesCount(filesCount);
            repository.setChunksCount(totalChunksCount);
            repository.setStatus(AnalysisStatus.READY);
            repository.setErrorMessage(null);
            repoRepository.save(repository);

            log.info("Analysis complete for repository {}: {} files scanned, {} chunks indexed",
                    repository.getFullName(), filesCount, totalChunksCount);

        } catch (Exception ex) {
            log.error("Analysis pipeline failed for repo ID {}", repositoryId, ex);
            repository.setStatus(AnalysisStatus.FAILED);
            repository.setErrorMessage("Analysis failed: " + ex.getMessage());
            repoRepository.save(repository);
        }
    }

    private void flushChunks(List<CodeChunk> chunks) {
        List<String> contents = chunks.stream().map(CodeChunk::getContent).toList();
        List<List<Double>> vectors = embeddingService.embedBatch(contents);

        for (int i = 0; i < chunks.size(); i++) {
            if (i < vectors.size()) {
                chunks.get(i).setEmbedding(embeddingService.vectorToString(vectors.get(i)));
            }
        }
        chunkRepository.saveAll(chunks);
    }

    private boolean shouldIgnorePath(String path) {
        if (path == null) return true;
        for (String ignored : IGNORED_DIRECTORIES) {
            if (path.startsWith(ignored + "/") || path.contains("/" + ignored + "/")) {
                return true;
            }
        }

        boolean hasValidExt = false;
        String lower = path.toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                hasValidExt = true;
                break;
            }
        }
        return !hasValidExt;
    }

    private String detectLanguage(String filePath) {
        if (filePath == null) return "text";
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".js") || lower.endsWith(".jsx")) return "javascript";
        if (lower.endsWith(".ts") || lower.endsWith(".tsx")) return "typescript";
        if (lower.endsWith(".py")) return "python";
        if (lower.endsWith(".go")) return "go";
        if (lower.endsWith(".rs")) return "rust";
        if (lower.endsWith(".sql")) return "sql";
        if (lower.endsWith(".html")) return "html";
        if (lower.endsWith(".css")) return "css";
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) return "yaml";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".md")) return "markdown";
        return "text";
    }

    public List<FileTreeNode> buildFileTree(Long repositoryId) {
        List<RepositoryFile> files = fileRepository.findByRepositoryIdOrderByFilePathAsc(repositoryId);
        FileTreeNode root = FileTreeNode.builder().name("root").path("").type("dir").children(new ArrayList<>()).build();

        for (RepositoryFile f : files) {
            String[] parts = f.getFilePath().split("/");
            FileTreeNode current = root;
            StringBuilder currentPath = new StringBuilder();

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                boolean isLast = (i == parts.length - 1);
                if (currentPath.length() > 0) currentPath.append("/");
                currentPath.append(part);

                if (isLast) {
                    current.getChildren().add(FileTreeNode.builder()
                            .name(part)
                            .path(f.getFilePath())
                            .type("file")
                            .size(f.getFileSize())
                            .children(new ArrayList<>())
                            .build());
                } else {
                    FileTreeNode existingDir = current.getChildren().stream()
                            .filter(n -> "dir".equals(n.getType()) && n.getName().equals(part))
                            .findFirst()
                            .orElse(null);

                    if (existingDir == null) {
                        existingDir = FileTreeNode.builder()
                                .name(part)
                                .path(currentPath.toString())
                                .type("dir")
                                .children(new ArrayList<>())
                                .build();
                        current.getChildren().add(existingDir);
                    }
                    current = existingDir;
                }
            }
        }

        return root.getChildren();
    }
}
