package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;

import org.bgf.youtube.fetcher.quota.QuotaManager;
import org.bgf.youtube.storage.StorageManager;

import java.util.List;

public class PlaylistFetcher implements DataFetcher {
    private static final long MIN_DELAY_MS = 1200L;
    private final QuotaManager quotaManager = new QuotaManager(MIN_DELAY_MS);

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        var plReq = youtube.playlists().list(List.of("snippet,contentDetails"));
        plReq.setMine(true).setMaxResults(50L);
        String pTok;
        do {
            var plResp = quotaManager.executeWithQuotaRetry(() -> plReq.execute());
            for (var pl : plResp.getItems()) {
                System.out.println("Playlist: " + pl.getSnippet().getTitle());
                var itReq = youtube.playlistItems().list(List.of("snippet,contentDetails"));
                itReq.setPlaylistId(pl.getId()).setMaxResults(50L);
                String iTok;
                do {
                    var itRes = quotaManager.executeWithQuotaRetry(() -> itReq.execute());
                    storage.save("playlist_" + pl.getId(), itRes.getItems());
                    iTok = itRes.getNextPageToken();
                    itReq.setPageToken(iTok);
                    quotaManager.enforceRateLimit();
                } while (iTok != null);
            }
            pTok = plResp.getNextPageToken();
            plReq.setPageToken(pTok);
            quotaManager.enforceRateLimit();
        } while (pTok != null);
    }

    @Override
    public String toString() {
        return "Playlists";
    }
}