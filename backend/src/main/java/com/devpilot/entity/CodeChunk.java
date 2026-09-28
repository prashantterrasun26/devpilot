package com.devpilot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "code_chunks", indexes = {
    @Index(name = "idx_code_chunks_repo_id", columnList = "repository_id"),
    @Index(name = "idx_code_chunks_file_path", columnList = "file_path")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_file_id")
    private RepositoryFile repositoryFile;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    private String language;

    @Column(name = "symbol_name")
    private String symbolName;

    @Column(name = "class_name")
    private String className;

    @Column(name = "start_line")
    private Integer startLine;

    @Column(name = "end_line")
    private Integer endLine;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Stored as a formatted vector string or float array string for pgvector.
     * In PostgreSQL with pgvector: vector(1536) column.
     */
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "commit_sha")
    private String commitSha;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
