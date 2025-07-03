// src/test/java/org/bgf/youtube/fetcher/VideoDetailFetcherTest.java
package org.bgf.youtube.fetcher;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.bgf.youtube.storage.StorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoDetailFetcherTest {
    @Mock
    YouTube youtube;
    @Mock
    YouTube.Videos videos;
    @Mock
    YouTube.Videos.List videosList;
    @Mock
    StorageManager storage;

    private VideoDetailFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new VideoDetailFetcher();
    }

    @Test
    void fetch_downloadsAndStoresVideoDetails() throws Exception {
        // prepare two videos
        Video v1 = new Video().setId("vid1")
                .setSnippet(new VideoSnippet()
                        .setPublishedAt(new DateTime("2025-01-01T00:00:00Z"))
                        .setThumbnails(new ThumbnailDetails()
                                .setDefault(new Thumbnail().setUrl("http://u/def1"))
                                .setHigh(new Thumbnail().setUrl("http://u/high1"))
                        )
                );
        Video v2 = new Video().setId("vid2")
                .setSnippet(new VideoSnippet()
                        .setPublishedAt(new DateTime("2025-02-02T00:00:00Z"))
                        .setThumbnails(new ThumbnailDetails()
                                .setDefault(new Thumbnail().setUrl("http://u/def2"))
                                .setHigh(new Thumbnail().setUrl("http://u/high2"))
                        )
                );
        VideoListResponse resp = new VideoListResponse()
                .setItems(Arrays.asList(v1, v2));

        when(youtube.videos()).thenReturn(videos);
        when(videos.list(List.of("snippet"))).thenReturn(videosList);
        when(videosList.setId(List.of("vid1", "vid2"))).thenReturn(videosList);
        when(videosList.execute()).thenReturn(resp);
        when(storage.downloadImage("vid1", anyString())).thenReturn("stored/path.jpg");

        fetcher.fetch(youtube, storage);

        // each thumbnail URL downloaded
        verify(storage).downloadImage("vid1", "http://u/def1");
        verify(storage).downloadImage("vid1", "http://u/high1");
        verify(storage).downloadImage("vid2", "http://u/def2");
        verify(storage).downloadImage("vid2", "http://u/high2");
        // each video detail saved once
        verify(storage, times(2)).save(eq("videoDetails"), any());
    }
}
