package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;
import org.bgf.youtube.storage.StorageManager;
import org.bgf.youtube.fetcher.util.YouTubeApiManager;
import org.bgf.youtube.PromptService;

import java.io.IOException;
import java.util.List;

public class SubtitleInfoFetcher implements DataFetcher {

    private final YouTubeApiManager apiManager = new YouTubeApiManager();

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        String videoId = PromptService.prompt("Enter video ID for captions: ");
        var req = youtube.captions().list(List.of("snippet"), videoId);
        var resp = apiManager.executeWithErrorCheck(req::execute);
        for (var caption : resp.getItems()) {
            System.out.printf("Caption: %s, Language: %s, Name: %s\n",
                    caption.getId(), caption.getSnippet().getLanguage(), caption.getSnippet().getName());
        }
        if (resp.getItems().isEmpty()) {
            System.out.printf("No caption found for: %s\n", videoId);
            return;
        }
        try {
            storage.save("captions_" + videoId, resp.getItems());
            apiManager.enforceRateLimit();
        } catch (IOException e) {
            System.err.println("Failed storing the result. " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "Captions";
    }
}