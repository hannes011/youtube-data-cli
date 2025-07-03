package org.bgf.youtube.fetcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.youtube.YouTube;
import org.bgf.youtube.storage.StorageManager;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class SubtitleInfoFetcher implements DataFetcher {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        var dir = Path.of("data");
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().startsWith("thumb_"))
                    .forEach(p -> checkCaptions(p, youtube));
        }
    }

    private void checkCaptions(Path thumbFile, YouTube youtube) {
        var id = thumbFile.getFileName().toString().split("_")[1];
        try {
            var resp = youtube.captions().list(List.of("snippet"), id).setVideoId(id).execute();
            System.out.println(id + " captions: " + !resp.getItems().isEmpty());
        } catch (IOException e) {
            System.err.println("Caption check failed for " + id);
        }
    }
}