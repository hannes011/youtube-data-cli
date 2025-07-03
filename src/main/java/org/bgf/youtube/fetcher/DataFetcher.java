package org.bgf.youtube.fetcher;

import com.google.api.services.youtube.YouTube;
import org.bgf.youtube.storage.StorageManager;

public interface DataFetcher {
    /**
     * Fetch data using YouTube client and store via StorageManager.
     * Should present paths or data on console.
     */
    void fetch(YouTube youtube, StorageManager storage) throws Exception;
}