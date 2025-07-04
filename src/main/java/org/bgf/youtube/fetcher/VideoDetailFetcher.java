package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.google.api.services.youtube.model.Video;

import com.google.api.services.youtube.model.VideoListResponse;
import org.bgf.youtube.storage.StorageManager;
import org.bgf.youtube.fetcher.util.PaginationService;

import java.io.IOException;
import java.util.List;

public class VideoDetailFetcher implements DataFetcher {

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        var list = youtube.videos().list(List.of("snippet", "contentDetails"));
        PaginationService.paginateStream(
            list,
                VideoListResponse::getNextPageToken,
                YouTube.Videos.List::setPageToken
        ).forEach(resp -> {
            for (var vid : resp.getItems()) {
                var id = vid.getId();
                System.out.printf("ID: %s, URL: https://youtu.be/%s, Published: %s, Captions: %s\n",
                        id, id, vid.getSnippet().getPublishedAt(), vid.getContentDetails().getCaption());
                var thumbs = vid.getSnippet().getThumbnails();
                downloadThumbs(id, thumbs, storage);
            }
            storeVideos(storage, "video_details", resp.getItems());
        });
    }

    private void downloadThumbs(String id, ThumbnailDetails thumbs, StorageManager storage) {
        if (thumbs != null && thumbs.getDefault() != null) {
            var url = thumbs.getDefault().getUrl();
            try {
                var path = storage.downloadImage("thumb_" + id, url);
                System.out.println("Thumbnail default image saved: " + path);
                if (thumbs.getHigh() != null) {
                    path = storage.downloadImage("thumb_" + id, thumbs.getHigh().getUrl());
                    System.out.println("Thumbnail default image saved: " + path);
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void storeVideos(StorageManager storage, String storageKey, List<Video> items) {
        try {
            storage.save(storageKey, items);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "Video details";
    }
}