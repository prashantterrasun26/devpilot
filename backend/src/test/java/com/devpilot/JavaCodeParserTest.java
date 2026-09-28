package com.devpilot;

import com.devpilot.parser.JavaCodeParser;
import com.devpilot.parser.RawCodeChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JavaCodeParserTest {

    private final JavaCodeParser parser = new JavaCodeParser();

    @Test
    void testSupportsJava() {
        assertTrue(parser.supports("java", "Test.java"));
        assertTrue(parser.supports(null, "src/main/App.java"));
        assertFalse(parser.supports("python", "test.py"));
    }

    @Test
    void testParseJavaMethodsAndClasses() {
        String code = """
            package com.example;
            
            public class UserController {
            
                public ResponseEntity<User> getUser(Long id) {
                    return ResponseEntity.ok(new User());
                }
                
                public void deleteUser(Long id) {
                    userRepo.deleteById(id);
                }
            }
            """;

        List<RawCodeChunk> chunks = parser.parse("UserController.java", code);
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        assertEquals("UserController", chunks.get(0).getClassName());
    }
}
