package org.bgf.youtube.fetcher.util;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;

public class QuotaManager {
    public static final long DEFAULT_MIN_DELAY = 1200L;

    private final long minDelayMs;
    private long lastRequestTime = 0;

    public QuotaManager(long minDelayMs) {
        this.minDelayMs = minDelayMs;
    }

    public QuotaManager() {
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

    public <T> T executeWithQuotaCheck(QuotaCallable<T> callable) {
        try {
            return callable.call();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 403 && e.getDetails() != null &&
                    e.getDetails().getErrors() != null &&
                    e.getDetails().getErrors().stream().anyMatch(err -> "quotaExceeded".equals(err.getReason()))) {
                System.err.println("\n[YouTube API] Quota exceeded.\n" +
                        "Check your quota usage here: https://console.cloud.google.com/apis/api/youtube.googleapis.com/quotas\n" +
                        "If you need more quota, you can apply here: https://support.google.com/youtube/contact/yt_api_form\n");
            }
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface QuotaCallable<T> {
        T call() throws Exception;
    }
} 