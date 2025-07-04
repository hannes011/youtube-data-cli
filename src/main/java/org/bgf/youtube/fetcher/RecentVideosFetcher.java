package org.bgf.youtube.fetcher;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import org.bgf.youtube.fetcher.util.QuotaManager;
import org.bgf.youtube.storage.StorageManager;
import org.bgf.youtube.fetcher.util.PaginationService;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Scanner;

public class RecentVideosFetcher implements DataFetcher {
    private static final long MIN_DELAY_MS = 1200L;
    private final QuotaManager quotaManager = new QuotaManager(MIN_DELAY_MS);

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        var sc = new Scanner(System.in);
        System.out.print("Days back: ");
        int days = Integer.parseInt(sc.nextLine());
        System.out.print("Language code: ");
        String lang = sc.nextLine();

        var after = new DateTime(Instant.now().minusSeconds(days * 86400L).toEpochMilli()).toString();
        var req = youtube.search().list(List.of("snippet"));
        req.setPublishedAfter(after)
                .setType(List.of("video"))
                .setRelevanceLanguage(lang)
                .setMaxResults(50L);
        PaginationService.paginateStream(
            req,
                SearchListResponse::getNextPageToken,
                YouTube.Search.List::setPageToken
        ).forEach(resp -> storeSearchResult(storage, "recent_videos_" + lang + "_" + days, resp.getItems()));
        System.out.println("Completed recent videos fetch.");
    }

    private void storeSearchResult(StorageManager storage, String storageKey, List<SearchResult> items) {
        try {
            storage.save(storageKey, items);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "Recent Videos";
    }
}