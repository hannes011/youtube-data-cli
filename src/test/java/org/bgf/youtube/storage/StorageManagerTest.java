package org.bgf.youtube.storage;

import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StorageManagerTest {
    private StorageManager storage;
    private static final String BASE = "data";

    @BeforeEach
    void setup() throws Exception {
        storage = new StorageManager();
    }

    @Test
    void testSaveWritesFile() throws Exception {
        TestData data = new TestData("abc", 123);
        storage.save("test", data);

        List<File> files = Files.list(new File(BASE).toPath())
                .map(p -> p.toFile())
                .filter(f -> f.getName().startsWith("test_"))
                .toList();
        assertFalse(files.isEmpty(), "No file saved");
    }

    static class TestData {
        public String field;
        public int num;

        TestData(String f, int n) {
            field = f;
            num = n;
        }
    }
}