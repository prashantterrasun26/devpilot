package com.devpilot.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PythonCodeParser implements LanguageCodeParser {

    private static final Pattern DEF_PATTERN = Pattern.compile("(?m)^\\s*(?:async\\s+)?def\\s+([A-Za-z0-9_]+)\\s*\\(");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(?m)^\\s*class\\s+([A-Za-z0-9_]+)");

    @Override
    public boolean supports(String language, String filePath) {
        return (language != null && language.equalsIgnoreCase("python")) ||
                (filePath != null && filePath.endsWith(".py"));
    }

    @Override
    public List<RawCodeChunk> parse(String filePath, String content) {
        List<RawCodeChunk> chunks = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return chunks;
        }

        String[] lines = content.split("\r?\n");
        String currentClass = null;

        Matcher classMatcher = CLASS_PATTERN.matcher(content);
        if (classMatcher.find()) {
            currentClass = classMatcher.group(1);
        }

        int chunkSize = 60;
        int overlap = 10;
        int start = 0;

        while (start < lines.length) {
            int end = Math.min(start + chunkSize, lines.length);

            StringBuilder chunkBuilder = new StringBuilder();
            String detectedSymbol = null;

            for (int i = start; i < end; i++) {
                String line = lines[i];
                chunkBuilder.append(line).append("\n");

                if (detectedSymbol == null) {
                    Matcher defMatcher = DEF_PATTERN.matcher(line);
                    if (defMatcher.find()) {
                        detectedSymbol = defMatcher.group(1);
                    }
                }
            }

            chunks.add(RawCodeChunk.builder()
                    .filePath(filePath)
                    .language("python")
                    .className(currentClass)
                    .symbolName(detectedSymbol != null ? detectedSymbol : (currentClass != null ? currentClass : "module"))
                    .startLine(start + 1)
                    .endLine(end)
                    .content(chunkBuilder.toString().trim())
                    .metadata("{\"type\":\"python_block\"}")
                    .build());

            if (end == lines.length) {
                break;
            }
            start += (chunkSize - overlap);
        }

        return chunks;
    }
}
