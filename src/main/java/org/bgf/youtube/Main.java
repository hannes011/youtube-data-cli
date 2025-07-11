package org.bgf.youtube;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import org.bgf.youtube.auth.AuthManager;
import org.bgf.youtube.fetcher.DataFetcher;
import org.bgf.youtube.fetcher.FetcherException;
import org.bgf.youtube.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String APPLICATION_NAME = "YouTubeDataCli";

    public static void main(String[] args) {
        try {
            var authManager = new AuthManager();
            var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            var jsonFactory = GsonFactory.getDefaultInstance();
            var youtube = reauthenticate(authManager, httpTransport, jsonFactory);
            var storage = new StorageManager();
            List<DataFetcher> fetchers = List.of(
                    new org.bgf.youtube.fetcher.VideoDetailsFetcher(),
                    new org.bgf.youtube.fetcher.ChannelListFetcher(),
                    new org.bgf.youtube.fetcher.PlaylistDetailsFetcher(),
                    new org.bgf.youtube.fetcher.RecentChangesFetcher(),
                    new org.bgf.youtube.fetcher.SubscriberCountFetcher()
            );
            while (true) {
                List<Object> menuOptions = new ArrayList<>(fetchers);
                menuOptions.add("Change account");
                Object selected = PromptService.promptMenu("Choose an option:", menuOptions, "Exit");
                if (selected == null) break; // exit
                switch (selected) {
                    case String s:
                        youtube = reauthenticate(authManager, httpTransport, jsonFactory);
                        break;
                    case DataFetcher fetcher:
                        try {
                            fetcher.fetch(youtube, storage);
                        } catch (FetcherException e) {
                            logger.error(e.getMessage());
                        } catch (Exception e) {
                            logger.error("Error executing fetcher: {}", e.getMessage(), e);
                        }
                        break;
                    default:
                        logger.error("Unexpected selection: {}", selected);
                }
            }
        } catch (Exception e) {
            logger.error("Fatal error: {}", e.getMessage(), e);
            System.exit(1);
        }
        System.out.println("Exited.");
    }

    private static YouTube reauthenticate(AuthManager authManager, NetHttpTransport httpTransport, GsonFactory jsonFactory) throws GeneralSecurityException {
        var credential = authManager.authorize();
        if (credential == null || credential.getRefreshToken() == null) {
            logger.error("No valid refresh token available. Exiting.");
            System.exit(1);
        }
        return new YouTube.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}