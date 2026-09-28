package com.devpilot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "repository_files", indexes = {
    @Index(name = "idx_repo_files_repo_id", columnList = "repository_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositoryFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    private String language;

    @Column(name = "file_size")
    private Long fileSize;

    private String sha;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
