package org.bgf.youtube.fetcher.util;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import org.bgf.youtube.fetcher.FetcherException;

public class YouTubeApiManager {
    public static final long DEFAULT_MIN_DELAY = 1200L;

    private final long minDelayMs;
    private long lastRequestTime = 0;

    public YouTubeApiManager(long minDelayMs) {
        this.minDelayMs = minDelayMs;
    }

    public YouTubeApiManager() {
        this(DEFAULT_MIN_DELAY);
    }

    public void enforceRateLimit() {
        long now = System.currentTimeMillis();
        long wait = lastRequestTime + minDelayMs - now;
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }

    public <T> T executeWithErrorCheck(ExecutionCallable<T> callable) {
        try {
            return callable.call();
        } catch (GoogleJsonResponseException e) {
            if (e.getDetails() != null && e.getDetails().getErrors() != null) {
                if (e.getDetails().getErrors().stream().anyMatch(err -> "quotaExceeded".equals(err.getReason()))) {
                    throw new FetcherException("[YouTube API] Quota exceeded.\n" +
                            "Check your quota usage here: https://console.cloud.google.com/apis/api/youtube.googleapis.com/quotas\n" +
                            "If you need more quota, you can apply here: https://support.google.com/youtube/contact/yt_api_form\n", e);
                }
                //if (e.getDetails().getErrors().stream().anyMatch(err -> "insufficientPermissions".equals(err.getReason()))) {
                //     throw new FetcherException("[YouTube API] Insufficient permissions.\n" +
                //             "The requested operation requires additional scopes that were not granted during authentication.\n" +
                //             "Please delete the tokens/refresh_token.json file and re-authenticate to grant the required permissions.\n", e);
                //}
            }
            throw new FetcherException(e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface ExecutionCallable<T> {
        T call() throws Exception;
    }
} 