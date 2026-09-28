package com.devpilot.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawCodeChunk {
    private String filePath;
    private String language;
    private String symbolName;
    private String className;
    private Integer startLine;
    private Integer endLine;
    private String content;
    private String metadata;
}
