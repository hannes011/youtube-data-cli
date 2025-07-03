package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;
import org.bgf.youtube.storage.StorageManager;

import java.util.List;

public class SubscriptionFetcher implements DataFetcher {
    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        var subReq = youtube.subscriptions().list(List.of("snippet"));
        subReq.setMine(true).setMaxResults(50L);
        String tok;
        do {
            var subResp = subReq.execute();
            for (var sub : subResp.getItems()) {
                var chId = sub.getSnippet().getResourceId().getChannelId();
                System.out.println("Channel: " + sub.getSnippet().getTitle());
                var vidReq = youtube.search().list(List.of("snippet"));
                vidReq.setChannelId(chId)
                        .setType(List.of("video"))
                        .setMaxResults(50L);
                var vids = vidReq.execute();
                storage.save("subs_" + chId, vids.getItems());
            }
            tok = subResp.getNextPageToken();
            subReq.setPageToken(tok);
        } while (tok != null);
    }
}