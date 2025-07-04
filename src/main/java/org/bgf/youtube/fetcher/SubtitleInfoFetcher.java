package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;
import org.bgf.youtube.storage.StorageManager;
import org.bgf.youtube.fetcher.util.QuotaManager;

import java.util.List;
import java.util.Scanner;

public class SubtitleInfoFetcher implements DataFetcher {

    private final QuotaManager quotaManager = new QuotaManager();

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        String videoId = promptForVideoId();
        var req = youtube.captions().list(List.of("snippet"), videoId);
        var resp = quotaManager.executeWithQuotaCheck(req::execute);
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
            quotaManager.enforceRateLimit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("resource")
    private static String promptForVideoId() {
        System.out.print("Enter video ID for captions: ");
        return new Scanner(System.in).nextLine();
    }

    @Override
    public String toString() {
        return "Captions";
    }
}