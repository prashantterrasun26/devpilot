package com.devpilot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "repositories", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "github_repo_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "github_repo_id", nullable = false)
    private Long githubRepoId;

    @Column(nullable = false)
    private String name;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "owner_login", nullable = false)
    private String ownerLogin;

    @Column(length = 2000)
    private String description;

    @Column(name = "html_url")
    private String htmlUrl;

    @Column(name = "default_branch")
    private String defaultBranch;

    @Column(name = "is_private")
    private Boolean isPrivate;

    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AnalysisStatus status = AnalysisStatus.NOT_ANALYZED;

    @Column(name = "files_count")
    @Builder.Default
    private Integer filesCount = 0;

    @Column(name = "chunks_count")
    @Builder.Default
    private Integer chunksCount = 0;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "last_commit_sha")
    private String lastCommitSha;

    @Column(name = "architecture_mermaid", columnDefinition = "TEXT")
    private String architectureMermaid;

    @Column(name = "architecture_summary", columnDefinition = "TEXT")
    private String architectureSummary;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
