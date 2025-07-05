package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.google.api.services.youtube.model.Video;

import com.google.api.services.youtube.model.VideoListResponse;
import org.bgf.youtube.storage.StorageManager;
import org.bgf.youtube.fetcher.util.PaginationService;
import org.bgf.youtube.PromptService;

import java.io.IOException;
import java.util.List;

public class VideoDetailFetcher implements DataFetcher {

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        var list = youtube.videos().list(List.of("snippet", "contentDetails"));
        var vidIds = PromptService.promptList("Enter video ID(s) (comma-separated): ");
        list.setId(vidIds);
        PaginationService.paginateStream(list, VideoListResponse::getNextPageToken, YouTube.Videos.List::setPageToken).forEach(resp -> {
            for (var vid : resp.getItems()) {
                var id = vid.getId();
                System.out.printf("ID: %s, URL: https://youtu.be/%s, Published: %s, Captions: %s\n", id, id, vid.getSnippet().getPublishedAt(), vid.getContentDetails().getCaption());
                var thumbs = vid.getSnippet().getThumbnails();
                downloadThumbs(id, thumbs, storage);
                storeVideo(storage, "video_details_" + id, vid);
            }
        });
    }

    private void downloadThumbs(String id, ThumbnailDetails thumbs, StorageManager storage) {
        if (thumbs != null && thumbs.getDefault() != null) {
            var url = thumbs.getDefault().getUrl();
            try {
                var path = storage.downloadImage("thumb_" + id + "_min", url);
                System.out.println("Thumbnail default image saved: " + path);
                if (thumbs.getStandard() != null) {
                    path = storage.downloadImage("thumb_" + id + "_std", thumbs.getStandard().getUrl());
                    System.out.println("Thumbnail standard image saved: " + path);
                }
                if (thumbs.getMaxres() != null) {
                    path = storage.downloadImage("thumb_" + id + "_max", thumbs.getMaxres().getUrl());
                    System.out.println("Thumbnail maxres image saved: " + path);
                }
            } catch (IOException e) {
                System.err.println("Failed storing the result. " + e.getMessage());
            }
        }
    }

    private void storeVideo(StorageManager storage, String storageKey, Video vid) {
        try {
            storage.save(storageKey, vid);
        } catch (IOException e) {
            System.err.println("Failed storing the result. " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "Video details";
    }
}