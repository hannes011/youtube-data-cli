package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;

import org.bgf.youtube.fetcher.quota.QuotaManager;
import org.bgf.youtube.storage.StorageManager;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;

import java.util.List;

public class SubscriptionFetcher implements DataFetcher {
    private static final long MIN_DELAY_MS = 1200L; // 50 requests/minute = 1.2s/request
    private final QuotaManager quotaManager = new QuotaManager(MIN_DELAY_MS);

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        var subReq = youtube.subscriptions().list(List.of("snippet"));
        subReq.setMine(true).setMaxResults(50L);
        String tok;
        do {
            var subResp = quotaManager.executeWithQuotaRetry(() -> subReq.execute());
            for (var sub : subResp.getItems()) {
                var chId = sub.getSnippet().getResourceId().getChannelId();
                System.out.println("Channel: " + sub.getSnippet().getTitle());
                var vidReq = youtube.search().list(List.of("snippet"));
                vidReq.setChannelId(chId)
                        .setType(List.of("video"))
                        .setMaxResults(50L);
                var vids = quotaManager.executeWithQuotaRetry(() -> vidReq.execute());
                storage.save("subs_" + chId, vids.getItems());
                quotaManager.enforceRateLimit();
            }
            tok = subResp.getNextPageToken();
            subReq.setPageToken(tok);
            quotaManager.enforceRateLimit();
        } while (tok != null);
    }

    @Override
    public String toString() {
        return "Subscriptions";
    }

}