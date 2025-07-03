package org.bgf.youtube.fetcher;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import org.bgf.youtube.storage.StorageManager;

import java.time.Instant;
import java.util.List;
import java.util.Scanner;

public class RecentVideosFetcher implements DataFetcher {
    @SuppressWarnings("resource")
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

        String tok;
        do {
            SearchListResponse resp = req.execute();
            storage.save("recent_" + lang + "_" + days, resp.getItems());
            tok = resp.getNextPageToken();
            req.setPageToken(tok);
        } while (tok != null);
        System.out.println("Completed recent videos fetch.");
    }
}