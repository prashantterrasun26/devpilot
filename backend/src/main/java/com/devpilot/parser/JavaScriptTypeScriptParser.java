package com.devpilot.parser;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JavaScriptTypeScriptParser implements LanguageCodeParser {

    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(?m)^(?:export\\s+)?(?:async\\s+)?function\\s+([A-Za-z0-9_$]+)|(?:const|let|var)\\s+([A-Za-z0-9_$]+)\\s*=\\s*(?:async\\s*)?\\([^)]*\\)\\s*=>|(?:const|let|var)\\s+([A-Za-z0-9_$]+)\\s*=\\s*function");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(?m)^(?:export\\s+)?(?:default\\s+)?class\\s+([A-Za-z0-9_$]+)");

    @Override
    public boolean supports(String language, String filePath) {
        if (filePath == null) return false;
        String lower = filePath.toLowerCase();
        return lower.endsWith(".js") || lower.endsWith(".jsx") || lower.endsWith(".ts") || lower.endsWith(".tsx");
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
                    Matcher fnMatcher = FUNCTION_PATTERN.matcher(line);
                    if (fnMatcher.find()) {
                        detectedSymbol = fnMatcher.group(1) != null ? fnMatcher.group(1)
                                : (fnMatcher.group(2) != null ? fnMatcher.group(2) : fnMatcher.group(3));
                    }
                }
            }

            String lang = (filePath.endsWith(".ts") || filePath.endsWith(".tsx")) ? "typescript" : "javascript";

            chunks.add(RawCodeChunk.builder()
                    .filePath(filePath)
                    .language(lang)
                    .className(currentClass)
                    .symbolName(detectedSymbol != null ? detectedSymbol : (currentClass != null ? currentClass : "module-block"))
                    .startLine(start + 1)
                    .endLine(end)
                    .content(chunkBuilder.toString().trim())
                    .metadata("{\"type\":\"" + lang + "_block\"}")
                    .build());

            if (end == lines.length) {
                break;
            }
            start += (chunkSize - overlap);
        }

        return chunks;
    }
}
