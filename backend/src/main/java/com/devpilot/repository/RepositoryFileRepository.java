package com.devpilot.repository;

import com.devpilot.entity.RepositoryFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryFileRepository extends JpaRepository<RepositoryFile, Long> {
    List<RepositoryFile> findByRepositoryIdOrderByFilePathAsc(Long repositoryId);
    Optional<RepositoryFile> findByRepositoryIdAndFilePath(Long repositoryId, String filePath);
    void deleteByRepositoryId(Long repositoryId);
}
