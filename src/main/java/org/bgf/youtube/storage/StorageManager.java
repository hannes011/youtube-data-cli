package org.bgf.youtube.storage;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.URL;
import java.nio.file.*;

public class StorageManager {
    private static final String BASE_DIR = "data";
    private final ObjectMapper mapper = new ObjectMapper();

    public StorageManager() throws IOException {
        Files.createDirectories(Path.of(BASE_DIR));
    }

    public <T> void save(String key, T data) throws IOException {
        var file = Path.of(BASE_DIR, key + ".json");
        try (var out = Files.newBufferedWriter(file)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(out, data);
        }
        System.out.println("Saved: " + file);
    }

    @SuppressWarnings("deprecation")
    public String downloadImage(String key, String imageUrl) throws IOException {
        // Extract extension without query
        int dotIdx = imageUrl.lastIndexOf('.');
        int qIdx = imageUrl.indexOf('?', dotIdx);
        int endIdx = imageUrl.length();
        if (qIdx != -1) endIdx = qIdx;
        String ext = dotIdx != -1 ? imageUrl.substring(dotIdx, endIdx) : "";
        var file = Path.of(BASE_DIR, key + ext);
        try (var in = new URL(imageUrl).openStream();
             var out = Files.newOutputStream(file, StandardOpenOption.CREATE)) {
            in.transferTo(out);
        }
        return file.toString();
    }
}