package com.devpilot.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JavaCodeParser implements LanguageCodeParser {

    private static final Pattern CLASS_PATTERN = Pattern.compile("(?m)^(?:\\s*(?:public|protected|private|abstract|static|final)\\s+)*(?:class|interface|enum|record)\\s+([A-Za-z0-9_]+)");
    private static final Pattern METHOD_PATTERN = Pattern.compile("(?m)^\\s*(?:@\\w+(?:\\([^)]*\\))?\\s*)*(?:public|protected|private|static|final|synchronized|abstract|default|native|transient)\\s+[\\w<>\\[\\],\\s]+\\s+([A-Za-z0-9_]+)\\s*\\(");

    @Override
    public boolean supports(String language, String filePath) {
        return (language != null && language.equalsIgnoreCase("java")) ||
                (filePath != null && filePath.endsWith(".java"));
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

        // Segment file into logical blocks (methods or chunk windows)
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
                    Matcher methodMatcher = METHOD_PATTERN.matcher(line);
                    if (methodMatcher.find()) {
                        detectedSymbol = methodMatcher.group(1);
                    }
                }
            }

            chunks.add(RawCodeChunk.builder()
                    .filePath(filePath)
                    .language("java")
                    .className(currentClass)
                    .symbolName(detectedSymbol != null ? detectedSymbol : (currentClass != null ? currentClass : "class-block"))
                    .startLine(start + 1)
                    .endLine(end)
                    .content(chunkBuilder.toString().trim())
                    .metadata("{\"type\":\"java_block\",\"class\":\"" + (currentClass != null ? currentClass : "") + "\"}")
                    .build());

            if (end == lines.length) {
                break;
            }
            start += (chunkSize - overlap);
        }

        return chunks;
    }
}
