// src/test/java/org/bgf/youtube/fetcher/RecentVideosFetcherTest.java
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecentVideosFetcherTest {
    @Mock
    YouTube youtube;
    @Mock
    YouTube.Search search;
    @Mock
    YouTube.Search.List searchList;
    @Mock
    StorageManager storage;

    private RecentVideosFetcher fetcher;

    @BeforeEach
    void setUp() {
        // e.g. videos since 7 days, language "en"
        fetcher = new RecentVideosFetcher();
    }

    @Test
    void fetch_returnsOnlyRecentVideosInLanguage() throws Exception {
        String publishedAfter = DateTime.parseRfc3339(
                Instant.now().minus(7, ChronoUnit.DAYS).toString()
        ).toStringRfc3339();

        SearchResult sr = new SearchResult()
                .setId(new ResourceId().setVideoId("vidZ"))
                .setSnippet(new SearchResultSnippet().setPublishedAt(
                        new DateTime(Instant.now().toString())
                ));
        SearchListResponse resp = new SearchListResponse()
                .setItems(Collections.singletonList(sr));

        when(youtube.search()).thenReturn(search);
        when(search.list(List.of("snippet"))).thenReturn(searchList);
        when(searchList.setPublishedAfter(publishedAfter)).thenReturn(searchList);
        when(searchList.setRelevanceLanguage("en")).thenReturn(searchList);
        when(searchList.execute()).thenReturn(resp);

        fetcher.fetch(youtube, storage);

        verify(storage).save(eq("recentVideos"), any());
    }
}
