package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
public final class ParallelWebCrawler implements WebCrawler {
    private final Clock clock;
    private final List<Pattern> ignoredUrls;
    private final int maxDepth;
    private final PageParserFactory parserFactory;
    private final ForkJoinPool pool;
    private final int popularWordCount;
    private final Duration timeout;

    /**
     * Constructs a ParallelWebCrawler with the provided dependencies.
     *
     * @param clock            the clock used to track time
     * @param timeout          the maximum timeout duration for crawling
     * @param popularWordCount the number of popular words to track
     * @param threadCount      the number of threads to use for parallel processing
     * @param ignoredUrls      the list of ignored URL patterns
     * @param maxDepth         the maximum depth for crawling
     * @param parserFactory    the factory for creating page parsers
     */
    @Inject
    public ParallelWebCrawler(
            Clock clock,
            @Timeout Duration timeout,
            @PopularWordCount int popularWordCount,
            @TargetParallelism int threadCount,
            @IgnoredUrls List<Pattern> ignoredUrls,
            @MaxDepth int maxDepth,
            PageParserFactory parserFactory) {
        this.clock = clock;
        this.timeout = timeout;
        this.popularWordCount = popularWordCount;
        this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
        this.ignoredUrls = ignoredUrls;
        this.maxDepth = maxDepth;
        this.parserFactory = parserFactory;
    }

    /**
     * Crawls the web starting from the provided URLs.
     *
     * @param startingUrls the list of URLs to start crawling from
     * @return the crawl result
     */
    @Override
    public CrawlResult crawl(List<String> startingUrls) {
        Instant deadline = clock.instant().plus(timeout);
        ConcurrentMap<String, Integer> wordCounts = new ConcurrentHashMap<>();
        ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();

        // Process each starting URL in parallel using the ForkJoinPool
        startingUrls.forEach(url -> pool.invoke(new ParallelWebCrawlerTask(url, deadline, maxDepth, wordCounts, visitedUrls,
                        parserFactory, clock, ignoredUrls)));

        // Build and return the crawl result
        return wordCounts.isEmpty()
                ? new CrawlResult.Builder()
                // If the wordCounts is empty, create a new CrawlResult.Builder
                .setWordCounts(wordCounts)
                // Set the word counts to the empty wordCounts map
                .setUrlsVisited(visitedUrls.size())
                // Set the number of visited URLs to the size of visitedUrls
                .build()
                // Build the CrawlResult
                : new CrawlResult.Builder()
                // If wordCounts is not empty, create a new CrawlResult.Builder
                .setWordCounts(WordCounts.sort(wordCounts, popularWordCount))
                // Set the sorted word counts using WordCounts.sort method
                .setUrlsVisited(visitedUrls.size())
                // Set the number of visited URLs to the size of visitedUrls
                .build();

    }

    /**
     * Gets the maximum parallelism supported by the system.
     *
     * @return the maximum parallelism
     */
    @Override
    public int getMaxParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }
}
