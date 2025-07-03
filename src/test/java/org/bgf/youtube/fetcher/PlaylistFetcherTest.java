// src/test/java/org/bgf/youtube/fetcher/PlaylistFetcherTest.java
package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.bgf.youtube.storage.StorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistFetcherTest {
    @Mock
    YouTube youtube;
    @Mock
    YouTube.Playlists playlists;
    @Mock
    YouTube.Playlists.List playlistsList;
    @Mock
    YouTube.PlaylistItems playlistItems;
    @Mock
    YouTube.PlaylistItems.List playlistItemsList;
    @Mock
    StorageManager storage;

    private PlaylistFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new PlaylistFetcher();
    }

    @Test
    void fetch_listsAllPlaylistsWithTheirVideos() throws Exception {
        Playlist pl = new Playlist()
                .setId("pl1")
                .setSnippet(new PlaylistSnippet().setTitle("My Playlist"));
        PlaylistListResponse plResp = new PlaylistListResponse()
                .setItems(Collections.singletonList(pl));

        PlaylistItem pi = new PlaylistItem()
                .setContentDetails(new PlaylistItemContentDetails().setVideoId("vidX"))
                .setSnippet(new PlaylistItemSnippet().setTitle("Video X"));
        PlaylistItemListResponse piResp = new PlaylistItemListResponse()
                .setItems(Collections.singletonList(pi));

        when(youtube.playlists()).thenReturn(playlists);
        when(playlists.list(List.of("snippet"))).thenReturn(playlistsList);
        when(playlistsList.setMine(true)).thenReturn(playlistsList);
        when(playlistsList.execute()).thenReturn(plResp);

        when(youtube.playlistItems()).thenReturn(playlistItems);
        when(playlistItems.list(List.of("snippet", "contentDetails"))).thenReturn(playlistItemsList);
        when(playlistItemsList.setPlaylistId("pl1")).thenReturn(playlistItemsList);
        when(playlistItemsList.execute()).thenReturn(piResp);

        fetcher.fetch(youtube, storage);

        // Should save playlists and items
        verify(storage).save(eq("playlists"), any());
    }
}
