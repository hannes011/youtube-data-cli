package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;
import org.bgf.youtube.storage.StorageManager;

import java.util.List;

public class PlaylistFetcher implements DataFetcher {
    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        var plReq = youtube.playlists().list(List.of("snippet,contentDetails"));
        plReq.setMine(true).setMaxResults(50L);
        String pTok;
        do {
            var plResp = plReq.execute();
            for (var pl : plResp.getItems()) {
                System.out.println("Playlist: " + pl.getSnippet().getTitle());
                var itReq = youtube.playlistItems().list(List.of("snippet,contentDetails"));
                itReq.setPlaylistId(pl.getId()).setMaxResults(50L);
                String iTok;
                do {
                    var itRes = itReq.execute();
                    storage.save("playlist_" + pl.getId(), itRes.getItems());
                    iTok = itRes.getNextPageToken();
                    itReq.setPageToken(iTok);
                } while (iTok != null);
            }
            pTok = plResp.getNextPageToken();
            plReq.setPageToken(pTok);
        } while (pTok != null);
    }
}