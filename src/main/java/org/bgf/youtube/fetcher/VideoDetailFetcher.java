package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;

import org.bgf.youtube.fetcher.quota.QuotaManager;
import org.bgf.youtube.storage.StorageManager;

import java.util.List;

public class VideoDetailFetcher implements DataFetcher {
    private static final long MIN_DELAY_MS = 1200L;
    private final QuotaManager quotaManager = new QuotaManager(MIN_DELAY_MS);

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        var list = youtube.videos().list(List.of("snippet", "contentDetails"));
//        list.setMine(true).setMaxResults(50L);
        String token;
        do {
            var resp = quotaManager.executeWithQuotaRetry(() -> list.execute());
            for (var vid : resp.getItems()) {
                var id = vid.getId();
                System.out.printf("ID: %s, URL: https://youtu.be/%s, Published: %s, Captions: %s\n",
                        id,
                        id,
                        vid.getSnippet().getPublishedAt(),
                        vid.getContentDetails().getCaption());

                var thumbs = vid.getSnippet().getThumbnails();
                if (thumbs != null && thumbs.getHigh() != null) {
                    var path = storage.downloadImage("thumb_" + id, thumbs.getHigh().getUrl());
                    System.out.println("Thumb saved: " + path);
                }
                quotaManager.enforceRateLimit();
            }
            token = resp.getNextPageToken();
            list.setPageToken(token);
            quotaManager.enforceRateLimit();
        } while (token != null);
    }

    @Override
    public String toString() {
        return "Video details";
    }
}