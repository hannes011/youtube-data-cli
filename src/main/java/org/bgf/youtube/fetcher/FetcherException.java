package org.bgf.youtube.fetcher;

public class FetcherException extends RuntimeException {
    public FetcherException(String message) {
        super(message);
    }

    public FetcherException(Throwable cause) {
        super(cause);
    }

    public FetcherException(String message, Throwable cause) {
        super(message, cause);
    }
}
