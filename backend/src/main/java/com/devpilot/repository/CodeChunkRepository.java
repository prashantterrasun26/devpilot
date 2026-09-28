package com.devpilot.repository;

import com.devpilot.entity.CodeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeChunkRepository extends JpaRepository<CodeChunk, Long> {

    List<CodeChunk> findByRepositoryId(Long repositoryId);

    void deleteByRepositoryId(Long repositoryId);

    /**
     * Native PostgreSQL vector similarity search using cosine distance (<=>).
     * If pgvector extension is present, uses vector distance.
     * Falls back to returning relevant chunks by keyword or repository if needed.
     */
    @Query(value = """
        SELECT c.*, (c.embedding <=> cast(:queryVector as vector)) as distance
        FROM code_chunks c
        WHERE c.repository_id = :repositoryId
          AND c.embedding IS NOT NULL
        ORDER BY distance ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<CodeChunk> findTopSimilarByVector(
        @Param("repositoryId") Long repositoryId,
        @Param("queryVector") String queryVector,
        @Param("limit") int limit
    );

    @Query("SELECT c FROM CodeChunk c WHERE c.repository.id = :repositoryId AND (LOWER(c.filePath) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.symbolName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<CodeChunk> searchByKeyword(@Param("repositoryId") Long repositoryId, @Param("keyword") String keyword);
}
