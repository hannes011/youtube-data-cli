package org.bgf.youtube.fetcher.quota;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;

public class QuotaManager {
    private final long minDelayMs;
    private long lastRequestTime = 0;

    public QuotaManager(long minDelayMs) {
        this.minDelayMs = minDelayMs;
    }

    public void enforceRateLimit() throws InterruptedException {
        long now = System.currentTimeMillis();
        long wait = lastRequestTime + minDelayMs - now;
        if (wait > 0) Thread.sleep(wait);
        lastRequestTime = System.currentTimeMillis();
    }

    public <T> T executeWithQuotaRetry(QuotaCallable<T> callable) throws Exception {
        try {
            return callable.call();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 403 && e.getDetails() != null &&
                    e.getDetails().getErrors() != null &&
                    e.getDetails().getErrors().stream().anyMatch(err -> "quotaExceeded".equals(err.getReason()))) {
                System.err.println("\n[YouTube API] Quota exceeded.\n" +
                        "Check your quota usage here: https://console.cloud.google.com/apis/api/youtube.googleapis.com/quotas\n" +
                        "If you need more quota, you can apply here: https://support.google.com/youtube/contact/yt_api_form\n");
                throw e;
            }
            throw e;
        }
    }

    @FunctionalInterface
    public interface QuotaCallable<T> {
        T call() throws Exception;
    }
} 