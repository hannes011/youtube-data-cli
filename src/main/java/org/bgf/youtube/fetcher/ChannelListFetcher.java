package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import org.bgf.youtube.PromptService;
import org.bgf.youtube.fetcher.util.PaginationService;
import org.bgf.youtube.storage.StorageManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelListFetcher implements DataFetcher {

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        Map<Runnable, String> options = new HashMap<>();
        options.put(() -> fetchMyAccount(youtube, storage), "My account");
        options.put(() -> fetchAccount(youtube, storage), "Other account");
        var fetchCallable = PromptService.promptMenu("Select account type:", options, "Cancel");
        if (fetchCallable == null) return;
        fetchCallable.run();
    }

    private void fetchAccount(YouTube youtube, StorageManager storage) {
        try {
            // Get channels for specific username
            String username = PromptService.prompt("Enter username (without @): ");
            YouTube.Channels.List channelReq = youtube.channels().list(List.of("snippet"));
            channelReq.setForUsername(username).setMaxResults(50L);
            PaginationService.paginateStream(
                    channelReq,
                    ChannelListResponse::getNextPageToken,
                    YouTube.Channels.List::setPageToken
            ).forEach(channelResp -> {
                for (var channel : channelResp.getItems()) { // TODO getItems is NULL, but why?
                    System.out.printf("Channel ID: %s, Title: %s\n",
                            channel.getId(), channel.getSnippet().getTitle());
                    storeChannel(storage, channel);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    
    }

    private void fetchMyAccount(YouTube youtube, StorageManager storage) {
        try {
            // Get my channels
            var channelReq = youtube.channels().list(List.of("snippet"));
            channelReq.setMine(true).setMaxResults(50L);
            PaginationService.paginateStream(
                    channelReq,
                    ChannelListResponse::getNextPageToken,
                    YouTube.Channels.List::setPageToken
            ).forEach(channelResp -> {
                for (var channel : channelResp.getItems()) {
                    System.out.printf("Channel ID: %s, Title: %s\n",
                            channel.getId(), channel.getSnippet().getTitle());
                    storeChannel(storage, channel);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void storeChannel(StorageManager storage, Channel channel) {
        try {
            storage.save("my_channels", channel);
        } catch (IOException e) {
            System.err.println("Failed storing the result. " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "Channel List";
    }
} 