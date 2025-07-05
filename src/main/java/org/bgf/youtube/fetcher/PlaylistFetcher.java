package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;

import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.PlaylistListResponse;
import org.bgf.youtube.fetcher.util.PaginationService;
import org.bgf.youtube.storage.StorageManager;

import java.io.IOException;
import java.util.List;

public class PlaylistFetcher implements DataFetcher {

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        var plReq = youtube.playlists().list(List.of("snippet", "contentDetails"));
        plReq.setMine(true).setMaxResults(50L);
        PaginationService.paginateStream(
                plReq,
                PlaylistListResponse::getNextPageToken,
                YouTube.Playlists.List::setPageToken
        ).forEach(plResp -> {
            for (var pl : plResp.getItems()) {
                System.out.println("Playlist: " + pl.getSnippet().getTitle());
                storePlaylist(storage, pl.getId(), pl);
            }
        });
    }

    private void fetchPlaylistItems(YouTube youtube, StorageManager storage, String playlistId) {
        try {
            var itReq = youtube.playlistItems().list(List.of("snippet", "contentDetails"));
            itReq.setPlaylistId(playlistId).setMaxResults(50L);
            PaginationService.paginateStream(
                    itReq,
                    PlaylistItemListResponse::getNextPageToken,
                    YouTube.PlaylistItems.List::setPageToken).forEach(itRes -> storePlaylistItems(storage, playlistId, itRes.getItems()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void storePlaylistItems(StorageManager storage, String playlistId, List<PlaylistItem> items) {
        try {
            storage.save("playlist_" + playlistId + "_items", items);
        } catch (IOException e) {
            System.err.println("Failed storing the result. " + e.getMessage());
        }
    }

    private void storePlaylist(StorageManager storage, String playlistId, Playlist pl) {
        try {
            storage.save("playlist_" + playlistId, pl);
        } catch (IOException e) {
            System.err.println("Failed storing the result. " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "Playlists";
    }
}