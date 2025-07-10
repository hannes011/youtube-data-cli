package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.bgf.youtube.PromptService;
import org.bgf.youtube.fetcher.util.PaginationService;
import org.bgf.youtube.storage.StorageManager;

import java.io.IOException;
import java.util.List;

public class PlaylistDetailsFetcher implements DataFetcher {

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        String channelId = PromptService.prompt("Enter channel ID: ");

        System.out.println("Playlists will contain 'private' videos if owned by you");
        // Get all playlists for the channel
        var playlistReq = youtube.playlists().list(List.of("snippet", "contentDetails"));
        playlistReq.setChannelId(channelId).setMaxResults(50L);
        PaginationService.paginateStream(
                playlistReq,
                PlaylistListResponse::getNextPageToken,
                YouTube.Playlists.List::setPageToken
        ).forEach(playlistResp -> {
            for (var playlist : playlistResp.getItems()) {
                System.out.printf("Playlist: %s\n", playlist.getSnippet().getTitle());

                // Get maxres thumbnail
                var thumbnails = playlist.getSnippet().getThumbnails();
                if (thumbnails != null && thumbnails.getMaxres() != null) {
                    System.out.printf("Maxres Thumbnail: %s\n", thumbnails.getMaxres().getUrl());
                }
                storePlaylist(storage, playlist.getId(), playlist);

                // Get all videos in this playlist
                try {
                    var itemsReq = youtube.playlistItems().list(List.of("snippet"));
                    itemsReq.setPlaylistId(playlist.getId()).setMaxResults(50L);
                    PaginationService.paginateStream(
                            itemsReq,
                            PlaylistItemListResponse::getNextPageToken,
                            YouTube.PlaylistItems.List::setPageToken
                    ).forEach(itemsResp -> {
                        System.out.println("Videos in playlist:");
                        for (var item : itemsResp.getItems()) {
                            System.out.printf("  - Video ID: %s, Title: %s\n",
                                    item.getSnippet().getResourceId().getVideoId(),
                                    item.getSnippet().getTitle());
                        }
                        storePlaylistItems(storage, playlist.getId(), itemsResp.getItems());
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                System.out.println("---");
            }
        });
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
        return "Playlist Details";
    }
} 