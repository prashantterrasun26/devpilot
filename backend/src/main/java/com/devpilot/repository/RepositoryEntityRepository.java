package com.devpilot.repository;

import com.devpilot.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface RepositoryEntityRepository extends JpaRepository<Repository, Long> {
    List<Repository> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<Repository> findByIdAndUserId(Long id, Long userId);
    Optional<Repository> findByUserIdAndGithubRepoId(Long userId, Long githubRepoId);
    Optional<Repository> findByUserIdAndFullName(Long userId, String fullName);
}
