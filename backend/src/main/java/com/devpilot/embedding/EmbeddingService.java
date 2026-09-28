package com.devpilot.embedding;

import java.util.List;

public interface EmbeddingService {

    List<Double> embedText(String text);

    List<List<Double>> embedBatch(List<String> texts);

    String vectorToString(List<Double> vector);
}
