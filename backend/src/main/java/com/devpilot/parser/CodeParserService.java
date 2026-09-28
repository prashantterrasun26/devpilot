package com.devpilot.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeParserService {

    private final List<LanguageCodeParser> parsers;
    private final GenericCodeParser fallbackParser;

    public List<RawCodeChunk> parseFile(String filePath, String language, String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        for (LanguageCodeParser parser : parsers) {
            if (!(parser instanceof GenericCodeParser) && parser.supports(language, filePath)) {
                log.debug("Using parser {} for {}", parser.getClass().getSimpleName(), filePath);
                return parser.parse(filePath, content);
            }
        }

        return fallbackParser.parse(filePath, content);
    }
}
