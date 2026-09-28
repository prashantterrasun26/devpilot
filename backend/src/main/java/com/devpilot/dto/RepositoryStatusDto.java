package com.devpilot.dto;

import com.devpilot.entity.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryStatusDto {
    private Long id;
    private AnalysisStatus status;
    private Integer filesScanned;
    private Integer chunksGenerated;
    private String errorMessage;
    private String lastCommitSha;
}
