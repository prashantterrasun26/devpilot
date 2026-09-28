package com.devpilot.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GenericCodeParser implements LanguageCodeParser {

    @Override
    public boolean supports(String language, String filePath) {
        return true; // fallback for all other supported file types (Go, C++, Rust, SQL, YAML, JSON, Markdown, etc.)
    }

    @Override
    public List<RawCodeChunk> parse(String filePath, String content) {
        List<RawCodeChunk> chunks = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return chunks;
        }

        String[] lines = content.split("\r?\n");
        int chunkSize = 70;
        int overlap = 15;
        int start = 0;

        String lang = detectLanguage(filePath);

        while (start < lines.length) {
            int end = Math.min(start + chunkSize, lines.length);

            StringBuilder chunkBuilder = new StringBuilder();
            for (int i = start; i < end; i++) {
                chunkBuilder.append(lines[i]).append("\n");
            }

            chunks.add(RawCodeChunk.builder()
                    .filePath(filePath)
                    .language(lang)
                    .className(null)
                    .symbolName("section-" + (start + 1))
                    .startLine(start + 1)
                    .endLine(end)
                    .content(chunkBuilder.toString().trim())
                    .metadata("{\"type\":\"generic_block\"}")
                    .build());

            if (end == lines.length) {
                break;
            }
            start += (chunkSize - overlap);
        }

        return chunks;
    }

    private String detectLanguage(String filePath) {
        if (filePath == null) return "text";
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".go")) return "go";
        if (lower.endsWith(".rs")) return "rust";
        if (lower.endsWith(".cpp") || lower.endsWith(".cc") || lower.endsWith(".c") || lower.endsWith(".h")) return "cpp";
        if (lower.endsWith(".cs")) return "csharp";
        if (lower.endsWith(".kt")) return "kotlin";
        if (lower.endsWith(".sql")) return "sql";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "html";
        if (lower.endsWith(".css") || lower.endsWith(".scss")) return "css";
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) return "yaml";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".md")) return "markdown";
        return "text";
    }
}
