package org.bgf.youtube;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import org.bgf.youtube.auth.AuthManager;
import org.bgf.youtube.fetcher.DataFetcher;
import org.bgf.youtube.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Scanner;

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
            Map<Integer, DataFetcher> menu = Map.of(
                    1, new org.bgf.youtube.fetcher.VideoDetailFetcher(),
                    2, new org.bgf.youtube.fetcher.PlaylistFetcher(),
                    3, new org.bgf.youtube.fetcher.SubscriptionFetcher(),
                    4, new org.bgf.youtube.fetcher.SubtitleInfoFetcher(),
                    5, new org.bgf.youtube.fetcher.RecentVideosFetcher()
            );
            try (var scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.println("Options:");
                    menu.forEach((k, v) -> System.out.printf("%d = %s\n", k, v.toString()));
                    System.out.println("0 = Exit");
                    System.out.print("Choose option: ");
                    int choice;
                    try {
                        choice = Integer.parseInt(scanner.nextLine());
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input, please enter a number.");
                        continue;
                    }
                    if (choice == 0) break;
                    var fetcher = menu.get(choice);
                    if (fetcher != null) {
                        try {
                            fetcher.fetch(youtube, storage);
                        } catch (Exception e) {
                            logger.error("Error executing fetcher {}: {}", choice, e.getMessage(), e);
                        }
                    } else {
                        System.out.println("Invalid choice, please select a valid option.");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Fatal error: {}", e.getMessage(), e);
            System.exit(1);
        }
        System.out.println("Exited.");
    }
}