// src/test/java/org/bgf/youtube/fetcher/SubscriptionFetcherTest.java
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
class SubscriptionFetcherTest {
    @Mock
    YouTube youtube;
    @Mock
    YouTube.Subscriptions subscriptions;
    @Mock
    YouTube.Subscriptions.List subsList;
    @Mock
    YouTube.PlaylistItems playlistItems;
    @Mock
    YouTube.PlaylistItems.List playlistItemsList;
    @Mock
    StorageManager storage;

    private SubscriberFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new SubscriberFetcher();
    }

    @Test
    void fetch_listsSubscribedChannelsAndTheirVideos() throws Exception {
        Subscription s = new Subscription()
                .setSnippet(new SubscriptionSnippet().setResourceId(
                        new ResourceId().setChannelId("chan1")));
        SubscriptionListResponse sResp = new SubscriptionListResponse()
                .setItems(Collections.singletonList(s));

        PlaylistItem pi = new PlaylistItem()
                .setContentDetails(new PlaylistItemContentDetails().setVideoId("vidY"));
        PlaylistItemListResponse piResp = new PlaylistItemListResponse()
                .setItems(Collections.singletonList(pi));

        when(youtube.subscriptions()).thenReturn(subscriptions);
        when(subscriptions.list(List.of("snippet"))).thenReturn(subsList);
        when(subsList.setMine(true)).thenReturn(subsList);
        when(subsList.execute()).thenReturn(sResp);

        when(youtube.playlistItems()).thenReturn(playlistItems);
        when(playlistItems.list(List.of("snippet", "contentDetails"))).thenReturn(playlistItemsList);
        when(playlistItemsList.setPlaylistId("UU" + "chan1".substring(2))) // uploads playlist
                .thenReturn(playlistItemsList);
        when(playlistItemsList.execute()).thenReturn(piResp);

        fetcher.fetch(youtube, storage);

        verify(storage).save(eq("subscriptions"), any());
    }
}
