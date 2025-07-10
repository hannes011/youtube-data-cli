package org.bgf.youtube;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import org.bgf.youtube.auth.AuthManager;
import org.bgf.youtube.fetcher.DataFetcher;
import org.bgf.youtube.fetcher.FetcherException;
import org.bgf.youtube.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String APPLICATION_NAME = "YouTubeDataCli";

    public static void main(String[] args) {
        try {
            var credential = AuthManager.authorize();
            if (credential == null || credential.getRefreshToken() == null) {
                logger.error("No valid refresh token available. Exiting.");
                System.exit(1);
            }
            var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            var jsonFactory = GsonFactory.getDefaultInstance();
            var youtube = new YouTube.Builder(httpTransport, jsonFactory, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            var storage = new StorageManager();
            List<DataFetcher> fetchers = List.of(
                new org.bgf.youtube.fetcher.VideoDetailsFetcher(),
                new org.bgf.youtube.fetcher.ChannelListFetcher(),
                new org.bgf.youtube.fetcher.PlaylistDetailsFetcher(),
                new org.bgf.youtube.fetcher.RecentChangesFetcher(),
                new org.bgf.youtube.fetcher.SubscriberCountFetcher()
            );
            while (true) {
                DataFetcher fetcher = PromptService.promptMenu("Choose an option:", fetchers, "Exit");
                if (fetcher == null) break; // exit
                try {
                    fetcher.fetch(youtube, storage);
                } catch (FetcherException e) {
                    logger.error(e.getMessage());
                } catch (Exception e) {
                    logger.error("Error executing fetcher: {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Fatal error: {}", e.getMessage(), e);
            System.exit(1);
        }
        System.out.println("Exited.");
    }
}