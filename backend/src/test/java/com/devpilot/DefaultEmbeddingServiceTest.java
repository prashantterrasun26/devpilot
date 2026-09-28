package com.devpilot;

import com.devpilot.embedding.DefaultEmbeddingService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultEmbeddingServiceTest {

    private final DefaultEmbeddingService embeddingService = new DefaultEmbeddingService();

    @Test
    void testEmbedTextDeterministic() {
        String sample = "public class AuthService { public void login() {} }";
        List<Double> vector1 = embeddingService.embedText(sample);
        List<Double> vector2 = embeddingService.embedText(sample);

        assertNotNull(vector1);
        assertEquals(1536, vector1.size());
        assertEquals(vector1, vector2); // Fallback is deterministic
    }

    @Test
    void testVectorToString() {
        List<Double> vector = List.of(0.123456, -0.654321);
        String s = embeddingService.vectorToString(vector);
        assertTrue(s.startsWith("[0.123456,-0.654321"));
    }
}
