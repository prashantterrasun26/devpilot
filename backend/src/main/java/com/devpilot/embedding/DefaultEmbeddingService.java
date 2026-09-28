package com.devpilot.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DefaultEmbeddingService implements EmbeddingService {

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    private static final int DEFAULT_DIMENSION = 1536;

    @Override
    public List<Double> embedText(String text) {
        if (text == null || text.isBlank()) {
            return generateDeterministicFallbackVector("", DEFAULT_DIMENSION);
        }

        if (embeddingModel != null && apiKey != null && !apiKey.isBlank() && !apiKey.equalsIgnoreCase("dummy-key")) {
            try {
                float[] floats = embeddingModel.embed(text);
                List<Double> result = new ArrayList<>(floats.length);
                for (float f : floats) {
                    result.add((double) f);
                }
                return result;
            } catch (Exception e) {
                log.warn("Spring AI embedding model call failed ({}). Using deterministic fallback embedding.", e.getMessage());
            }
        }

        return generateDeterministicFallbackVector(text, DEFAULT_DIMENSION);
    }

    @Override
    public List<List<Double>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        if (embeddingModel != null && apiKey != null && !apiKey.isBlank() && !apiKey.equalsIgnoreCase("dummy-key")) {
            try {
                List<float[]> embeddings = embeddingModel.embed(texts);
                List<List<Double>> result = new ArrayList<>(embeddings.size());
                for (float[] arr : embeddings) {
                    List<Double> dList = new ArrayList<>(arr.length);
                    for (float f : arr) {
                        dList.add((double) f);
                    }
                    result.add(dList);
                }
                return result;
            } catch (Exception e) {
                log.warn("Spring AI batch embedding call failed ({}). Falling back to deterministic embeddings.", e.getMessage());
            }
        }

        return texts.stream()
                .map(t -> generateDeterministicFallbackVector(t, DEFAULT_DIMENSION))
                .collect(Collectors.toList());
    }

    @Override
    public String vectorToString(List<Double> vector) {
        if (vector == null || vector.isEmpty()) {
            return "[]";
        }
        return "[" + vector.stream()
                .map(v -> String.format("%.6f", v))
                .collect(Collectors.joining(",")) + "]";
    }

    /**
     * Generates a normalized high-dimensional pseudo-semantic vector based on token frequencies and hashes.
     * This guarantees vector search mathematics (dot products, cosine similarities) operate properly in pgvector
     * even during local development without paid API keys.
     */
    private List<Double> generateDeterministicFallbackVector(String text, int dimension) {
        double[] vec = new double[dimension];
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            Random rng = new Random(new java.math.BigInteger(1, hash).longValue());

            String[] words = text.toLowerCase().split("\\W+");
            for (String word : words) {
                if (word.isBlank()) continue;
                int hashWord = Math.abs(word.hashCode()) % dimension;
                vec[hashWord] += 1.0;
            }

            for (int i = 0; i < dimension; i++) {
                vec[i] += (rng.nextDouble() - 0.5) * 0.1;
            }

            // Normalize vector to unit length
            double norm = 0.0;
            for (double v : vec) {
                norm += v * v;
            }
            norm = Math.sqrt(norm);
            if (norm > 0) {
                for (int i = 0; i < dimension; i++) {
                    vec[i] /= norm;
                }
            }
        } catch (Exception e) {
            log.error("Error generating fallback vector", e);
        }

        List<Double> result = new ArrayList<>(dimension);
        for (double v : vec) {
            result.add(v);
        }
        return result;
    }
}
