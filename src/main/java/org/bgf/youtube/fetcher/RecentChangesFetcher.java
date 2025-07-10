package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.bgf.youtube.PromptService;
import org.bgf.youtube.fetcher.util.PaginationService;
import org.bgf.youtube.storage.StorageManager;
import com.google.api.services.youtube.model.ActivityListResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RecentChangesFetcher implements DataFetcher {

    @Override
    public void fetch(YouTube youtube, StorageManager storage) throws Exception {
        int days = PromptService.promptInt("Enter number of days to look back: ");
        var cutoff = Instant.now().minusSeconds(days * 86400L);

        // Use PaginationService to iterate all channels
        var channelReq = youtube.channels().list(List.of("id", "snippet"));
        channelReq.setMine(true);
        List<Channel> allChannels = new ArrayList<>();
        PaginationService.paginateStream(
            channelReq,
            ChannelListResponse::getNextPageToken,
            YouTube.Channels.List::setPageToken
        ).forEach(channelResp -> allChannels.addAll(channelResp.getItems()));

        List<Activity> allActivities = new ArrayList<>();
        for (var channel : allChannels) {
            String channelId = channel.getId();
            var activitiesReq = youtube.activities().list(List.of("snippet", "contentDetails"));
            activitiesReq.setChannelId(channelId).setMaxResults(50L);
            boolean stop = false;
            var pages = PaginationService.paginateStream(
                activitiesReq,
                ActivityListResponse::getNextPageToken,
                YouTube.Activities.List::setPageToken
            ).iterator();
            while (pages.hasNext() && !stop) {
                var activitiesResp = pages.next();
                for (var activity : activitiesResp.getItems()) {
                    String type = activity.getSnippet().getType();
                    Instant published = Instant.ofEpochMilli(activity.getSnippet().getPublishedAt().getValue());
                    if (published.isBefore(cutoff) && type.equals("upload")) {
                        stop = true;
                        break;
                    }
                    allActivities.add(activity);
                }
            }
        }

        System.out.printf("Found %d recent video-related activities in the past %d days:\n", allActivities.size(), days);
        for (var activity : allActivities) {  // TODO grouping by resourceId + null filter
            var snippet = activity.getSnippet();
            String type = snippet.getType();
            if (type.equals("upload") || type.equals("playlistItem") || type.equals("update")) {
                var publishedAt = snippet.getPublishedAt();
                var channelId = snippet.getChannelId();
                String resourceId = null;
                String resourceType = "video";
                if (activity.getContentDetails() != null && activity.getContentDetails().getUpload() != null) {
                    resourceId = activity.getContentDetails().getUpload().getVideoId();
                } else if (activity.getContentDetails() != null && activity.getContentDetails().getPlaylistItem() != null) {
                    resourceId = activity.getContentDetails().getPlaylistItem().getResourceId().getVideoId();
                    resourceType = "playlist " + activity.getContentDetails().getPlaylistItem().getPlaylistId();
                }
                System.out.printf("Type: %s | Resource ID: %s | Resource Type: %s | Channel ID: %s | Date: %s\n",
                        type,
                        resourceId != null ? resourceId : "-",
                        resourceType,
                        channelId,
                        publishedAt);
            }
        }
        storeActivities(storage, "recent_changes_" + days + "_days", allActivities);
    }

    private void storeActivities(StorageManager storage, String storageKey, List<Activity> items) {
        try {
            storage.save(storageKey, items);
        } catch (IOException e) {
            System.err.println("Failed storing the result. " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "Recent Changes";
    }
} 