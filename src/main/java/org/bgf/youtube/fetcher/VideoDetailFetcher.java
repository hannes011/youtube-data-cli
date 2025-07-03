package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;
import org.bgf.youtube.storage.StorageManager;

import java.util.List;

public class VideoDetailFetcher implements DataFetcher {
    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        var list = youtube.videos().list(List.of("snippet", "contentDetails"));
//        list.setMine(true).setMaxResults(50L);
        String token;
        do {
            var resp = list.execute();
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
            }
            token = resp.getNextPageToken();
            list.setPageToken(token);
        } while (token != null);
    }
}