package org.bgf.youtube.fetcher;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.ChannelSnippet;
import org.bgf.youtube.auth.AuthManager;
import org.bgf.youtube.fetcher.util.PaginationService;
import org.bgf.youtube.fetcher.util.YouTubeApiManager;
import org.bgf.youtube.storage.StorageManager;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubscriberCountForAllAccountsFetcher implements DataFetcher {
    private final YouTubeApiManager apiManager = new YouTubeApiManager();
    private final AuthManager authManager;

    public SubscriberCountForAllAccountsFetcher(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        var flow = authManager.buildOfflineAuthFlow(httpTransport);
        List<AuthManager.RefreshTokenEntry> refreshTokens = authManager.loadRefreshTokens();
        System.out.println("Subscriber counts:");
        for (var token : refreshTokens) {
            try {
                Credential credential = authManager.refreshToken(flow, token);
                youtube = authManager.reauthenticate(credential);
                printSubscriberForAccountChannels(youtube);
            } catch (IOException e) {
                System.err.println("Failed to connect to YT account. " + e.getMessage());
            }
        }
    }

    private void printSubscriberForAccountChannels(YouTube youtube) throws IOException {
        var channelData = fetchMyAccountChannels(youtube);
        var channelIds = new ArrayList<>(channelData.keySet());

        var channelReq = youtube.channels().list(List.of("snippet", "statistics"));
        channelReq.setId(channelIds);
        var channelResp = apiManager.executeWithErrorCheck(channelReq::execute);

        for (var channel : channelResp.getItems()) {
            String channelId = channel.getId();
            String title = channel.getSnippet().getTitle();
            BigInteger subscriberCount = channel.getStatistics().getSubscriberCount();
            BigInteger videoCount = channel.getStatistics().getVideoCount();
            BigInteger viewCount = channel.getStatistics().getViewCount();
            BigInteger commentCount = channel.getStatistics().getCommentCount();

            System.out.printf("Channel: %s (%s) - Subscribers: %s - Videos: %s - Views: %s - Comments: %s\n",
                    title, channelId,
                    subscriberCount != null ? subscriberCount.toString() : "Hidden", videoCount, viewCount, commentCount);
        }
    }


    private Map<String, ChannelSnippet> fetchMyAccountChannels(YouTube youtube) {
        Map<String, ChannelSnippet> channelData = new HashMap<>();
        try {
            // Get my channels
            var channelReq = youtube.channels().list(List.of("snippet"));
            channelReq.setMine(true).setMaxResults(50L);
            PaginationService.paginateStream(
                    channelReq,
                    ChannelListResponse::getNextPageToken,
                    YouTube.Channels.List::setPageToken
            ).forEach(channelResp -> {
                if (channelResp.getItems() == null) {
                    System.err.println("Failed fetching channels");
                }
                for (var channel : channelResp.getItems()) {
                    channelData.put(channel.getId(), channel.getSnippet());
                }
            });
        } catch (IOException e) {
            System.err.println("Failed fetching channels");
        }
        return channelData;
    }

    @Override
    public String toString() {
        return "Subscriber Counts for all Accounts";
    }
} 