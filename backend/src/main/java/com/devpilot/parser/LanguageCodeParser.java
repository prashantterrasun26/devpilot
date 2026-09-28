package com.devpilot.parser;

import java.util.List;

public interface LanguageCodeParser {

    boolean supports(String language, String filePath);

    List<RawCodeChunk> parse(String filePath, String content);
}
