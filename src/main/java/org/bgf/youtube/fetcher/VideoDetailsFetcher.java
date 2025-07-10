package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.google.api.services.youtube.model.Video;
import org.bgf.youtube.PromptService;
import org.bgf.youtube.fetcher.util.PaginationService;
import org.bgf.youtube.fetcher.util.YouTubeApiManager;
import org.bgf.youtube.storage.StorageManager;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class VideoDetailsFetcher implements DataFetcher {
    private final YouTubeApiManager apiManager = new YouTubeApiManager();

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        String mode = PromptService.promptMenu("Which videos do you want to fetch?", List.of("All my videos", "Specific video ID(s)"), "Cancel");
        if (mode == null) return;
        if (mode.equals("All my videos")) {
            // Get uploads playlist ID
            var channelReq = youtube.channels().list(List.of("contentDetails"));
            channelReq.setMine(true);
            var channelResp = apiManager.executeWithErrorCheck(channelReq::execute);
            if (channelResp.getItems().isEmpty()) {
                System.out.println("No channel found for this account.");
                return;
            }
            String uploadsPlaylistId = channelResp.getItems().get(0).getContentDetails().getRelatedPlaylists().getUploads();
            // Get all video IDs from uploads playlist
            var playlistReq = youtube.playlistItems().list(List.of("snippet"));
            playlistReq.setPlaylistId(uploadsPlaylistId).setMaxResults(50L);
            List<String> videoIds = new ArrayList<>();
            PaginationService.paginateStream(
                playlistReq,
                PlaylistItemListResponse::getNextPageToken,
                YouTube.PlaylistItems.List::setPageToken
            ).forEach(playlistResp -> {
                for (var item : playlistResp.getItems()) {
                    videoIds.add(item.getSnippet().getResourceId().getVideoId());
                }
            });
            if (videoIds.isEmpty()) {
                System.out.println("No videos found for your channel.");
                return;
            }
            fetchVideos(youtube, storage, videoIds);
        } else {
            fetchVideos(youtube, storage, null);
        }
    }

    public void fetchVideos(YouTube youtube, StorageManager storage, List<String> videoIds) throws Exception {
        if (videoIds == null) {
            videoIds = PromptService.promptList("Enter video ID(s) (comma-separated): ");
        }
        for (int i = 0; i < videoIds.size(); i += 50) {
            List<String> batch = videoIds.subList(i, Math.min(i + 50, videoIds.size()));
            var videoReq = youtube.videos().list(List.of("snippet", "contentDetails", "statistics", "status"));
            videoReq.setId(batch);
            var videoResp = apiManager.executeWithErrorCheck(videoReq::execute);
            for (var video : videoResp.getItems()) {
                printVideoDetails(youtube, storage, video);
                downloadThumbs(video.getId(), video.getSnippet().getThumbnails(), storage);
                storeVideo(storage, "video_details_" + video.getId(), video);
            }
        }
    }

    private void printVideoDetails(YouTube youtube, StorageManager storage, Video video) {
        String videoId = video.getId();
        String audioLang = video.getSnippet().getDefaultAudioLanguage();
        String titleLang = video.getSnippet().getDefaultLanguage();
        System.out.printf("Video ID: %s\n", videoId);
        System.out.printf("Title: %s\n", video.getSnippet().getTitle());
        System.out.printf("Published: %s\n", video.getSnippet().getPublishedAt());
        System.out.printf("Privacy Status: %s\n", video.getStatus().getPrivacyStatus());
        System.out.printf("Upload Status: %s\n", video.getStatus().getUploadStatus());
        System.out.printf("Text Language: %s\n", titleLang);
        System.out.printf("Audio Language: %s\n", "zxx".equals(audioLang) ? "N/A" : audioLang);
        System.out.printf("Subtitle edited into video: %s\n", audioLang != null && titleLang != null && audioLang.length() > 1 && titleLang.length() > 1 && audioLang.substring(0, 2).equals(titleLang.substring(0, 2)) ? "probably no" : "probably yes");
        System.out.printf("Closed Captions Available: %s\n", video.getContentDetails().getCaption());
        System.out.printf("Duration: %s\n", formatDuration(video.getContentDetails().getDuration()));
        System.out.printf("URL: https://youtu.be/%s\n", videoId);
        System.out.printf("Channel ID: %s\n", video.getSnippet().getChannelId());

        // Get maxres thumbnail
        var thumbnails = video.getSnippet().getThumbnails();
        if (thumbnails != null && thumbnails.getMaxres() != null) {
            System.out.printf("Maxres Thumbnail: %s\n", thumbnails.getMaxres().getUrl());
        }

        // Get captions/subtitles
        try {
            var captionsReq = youtube.captions().list(List.of("snippet"), videoId);
            var captionsResp = apiManager.executeWithErrorCheck(captionsReq::execute);
            System.out.printf("Closed Captions Languages: %s\n", captionsResp.getItems().stream()
                    .map(c -> c.getSnippet().getLanguage())
                    .toList());
        } catch (Exception e) {
            //System.out.println("true".equals(video.getContentDetails().getCaption()) ? "Closed Captions: available, but no permission granted" : "Closed Captions: Not available");
        }

        System.out.println("---");
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

    private void storeVideo(StorageManager storage, String storageKey, Video video) {
        try {
            storage.save(storageKey, video);
        } catch (IOException e) {
            System.err.println("Failed storing the result. " + e.getMessage());
        }
    }

    private String formatDuration(String iso) {
        try {
            Duration d = Duration.parse(iso);
            long h = d.toHours();
            long m = d.toMinutesPart();
            long s = d.toSecondsPart();
            if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
            else return String.format("%d:%02d", m, s);
        } catch (Exception e) {
            return iso;
        }
    }

    @Override
    public String toString() {
        return "Video Details";
    }
} 