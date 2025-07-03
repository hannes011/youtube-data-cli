// src/test/java/org/bgf/youtube/fetcher/SubtitleInfoFetcherTest.java
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
class SubtitleInfoFetcherTest {
    @Mock
    YouTube youtube;
    @Mock
    YouTube.Captions captions;
    @Mock
    YouTube.Captions.List captionsList;
    @Mock
    StorageManager storage;

    private SubtitleInfoFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new SubtitleInfoFetcher();
    }

    @Test
    void fetch_flagsCaptionAvailability() throws Exception {
        Caption caption = new Caption().setId("cap1");
        CaptionListResponse resp = new CaptionListResponse()
                .setItems(Collections.singletonList(caption));

        when(youtube.captions()).thenReturn(captions);
        when(captions.list(List.of("snippet"), "vid1")).thenReturn(captionsList);
        when(captionsList.setVideoId("vid1")).thenReturn(captionsList);
        when(captionsList.execute()).thenReturn(resp);

        fetcher.fetch(youtube, storage);

        verify(storage).save(eq("captions"), any());
    }
}
