package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import org.bgf.youtube.storage.StorageManager;
import org.bgf.youtube.fetcher.util.PaginationService;
import org.bgf.youtube.PromptService;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class RecentVideosFetcher implements DataFetcher {

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        int days = PromptService.promptInt("Days back: ");
        String lang = PromptService.prompt("Language code: ");
        var after = Instant.now().minusSeconds(days * 86400L).toString();
        var req = youtube.search().list(List.of("snippet"));
        req.setPublishedAfter(after)
                .setType(List.of("video"))
                .setRelevanceLanguage(lang)
                .setMaxResults(50L);
        PaginationService.paginateStream(
                        req,
                        SearchListResponse::getNextPageToken,
                        YouTube.Search.List::setPageToken)
                .forEach(resp -> storeSearchResult(storage, "recent_videos_" + lang + "_" + days, resp.getItems()));
        System.out.println("Completed recent videos fetch.");
    }

    private void storeSearchResult(StorageManager storage, String storageKey, List<SearchResult> items) {
        try {
            storage.save(storageKey, items);
        } catch (IOException e) {
            System.err.println("Failed storing the result. " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "Recent Videos";
    }
}