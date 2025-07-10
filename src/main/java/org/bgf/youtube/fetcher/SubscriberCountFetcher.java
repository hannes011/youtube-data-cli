package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;
import org.bgf.youtube.PromptService;
import org.bgf.youtube.fetcher.util.YouTubeApiManager;
import org.bgf.youtube.storage.StorageManager;

import java.math.BigInteger;
import java.util.List;

public class SubscriberCountFetcher implements DataFetcher {
    private final YouTubeApiManager apiManager = new YouTubeApiManager();

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        var channelIds = PromptService.promptList("Enter channel ID(s) (comma-separated): ");

        var channelReq = youtube.channels().list(List.of("snippet", "statistics"));
        channelReq.setId(channelIds);
        var channelResp = apiManager.executeWithErrorCheck(channelReq::execute);

        System.out.println("Subscriber counts:");
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

        storage.save("subscriber_counts", channelResp.getItems());
    }

    @Override
    public String toString() {
        return "Subscriber Counts";
    }
} 