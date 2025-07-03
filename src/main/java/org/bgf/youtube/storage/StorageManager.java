package org.bgf.youtube.storage;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.time.Instant;

public class StorageManager {
    private static final String BASE_DIR = "data";
    private final ObjectMapper mapper = new ObjectMapper();

    public StorageManager() throws IOException {
        Files.createDirectories(Path.of(BASE_DIR));
    }

    public <T> void save(String key, T data) throws IOException {
        var file = Path.of(BASE_DIR, key + "_" + Instant.now().toEpochMilli() + ".json");
        try (var out = Files.newBufferedWriter(file)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(out, data);
        }
        System.out.println("Saved: " + file);
    }

    public String downloadImage(String key, String imageUrl) throws IOException {
        var ext = imageUrl.substring(imageUrl.lastIndexOf('.'));
        var file = Path.of(BASE_DIR, key + "_" + Instant.now().toEpochMilli() + ext);
        try (var in = new URL(imageUrl).openStream();
             var out = Files.newOutputStream(file, StandardOpenOption.CREATE)) {
            in.transferTo(out);
        }
        return file.toString();
    }
}