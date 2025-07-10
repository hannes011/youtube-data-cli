package org.bgf.youtube.fetcher.util;

import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.api.services.youtube.YouTubeRequest;

import java.util.Iterator;
import java.util.Spliterators;
import java.util.Spliterator;

public class PaginationService {

    /**
     * Returns a Stream over all pages of a paginated API.
     * @param initialRequest the initial request object (must be mutable for pageToken)
     * @param getNextPageToken function to extract the next page token from the response
     * @param setPageToken function to set the page token on the request
     * @param <Req> request type
     * @param <Resp> response type
     * @return Stream of responses (pages)
     */
    public static <Req extends YouTubeRequest<Resp>, Resp> Stream<Resp> paginateStream( // TODO make non-static
            Req initialRequest,
            Function<Resp, String> getNextPageToken,
            BiConsumer<Req, String> setPageToken
    ) {
        var apiManager = new YouTubeApiManager();
        Iterator<Resp> iterator = new Iterator<>() {
            private final Req req = initialRequest;
            private Resp nextResp = null;
            private boolean first = true;
            private boolean done = false;
            @Override
            public boolean hasNext() {
                if (done) return false;
                if (first) return true;
                String nextToken = getNextPageToken.apply(nextResp);
                if (nextToken == null || nextToken.isEmpty()) {
                    done = true;
                    return false;
                }
                //setPageToken.accept(req, nextToken);
                System.out.println("++ Next Page ++");
                return true;
            }
            @Override
            public Resp next() {
                if (first) {
                    first = false;
                } else {
                    String nextToken = getNextPageToken.apply(nextResp);
                    setPageToken.accept(req, nextToken);
                    apiManager.enforceRateLimit();
                }
                nextResp = apiManager.executeWithErrorCheck(req::execute);
                return nextResp;
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    @FunctionalInterface
    public interface BiConsumer<T, U> {
        void accept(T t, U u);
    }
}