package com.devpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceReference {
    private String file;
    private Integer startLine;
    private Integer endLine;
    private String symbolName;
    private String snippet;
}
