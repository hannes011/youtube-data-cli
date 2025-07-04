package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;

import com.google.api.services.youtube.model.SubscriptionListResponse;
import org.bgf.youtube.storage.StorageManager;
import org.bgf.youtube.fetcher.util.PaginationService;

import java.io.IOException;
import java.util.List;

public class SubscriptionFetcher implements DataFetcher {
    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        var subReq = youtube.subscriptions().list(List.of("snippet"));
        subReq.setMine(true).setMaxResults(50L);
        PaginationService.paginateStream(
            subReq,
                SubscriptionListResponse::getNextPageToken,
                YouTube.Subscriptions.List::setPageToken
        ).forEach(subResp -> {
            for (var sub : subResp.getItems()) {
                var chId = sub.getSnippet().getResourceId().getChannelId();
                System.out.println("Channel: " + chId + " (" + sub.getSnippet().getTitle() + ")");
                // var vidReq = youtube.search().list(List.of("snippet"));
                // vidReq.setChannelId(chId)
                //         .setType(List.of("video"))
                //         .setMaxResults(50L);
                // PaginationService.paginateStream(
                //     vidReq,
                //     req2 -> req2.execute(),
                //     vRes -> vRes.getNextPageToken(),
                //     (req2, tok) -> req2.setPageToken(tok))
                try {
                    storage.save("sub_" + chId, sub.getSnippet());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public String toString() {
        return "Subscriptions";
    }

}