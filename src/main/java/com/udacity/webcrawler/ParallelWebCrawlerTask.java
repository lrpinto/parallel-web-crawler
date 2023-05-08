package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * ParallelWebCrawlerTask represents a recursive task that crawls a given URL and its sub-links.
 */
public class ParallelWebCrawlerTask extends RecursiveTask<Boolean> {
    private final Clock clock;
    private final int currentDepth;
    private final Instant deadline;
    private final PageParserFactory parserFactory;
    private final String url;
    private final List<Pattern> urlsIgnored;
    private final ConcurrentSkipListSet<String> urlsVisited;
    private final ConcurrentMap<String, Integer> wordCounts;

    /**
     * Constructs an ParallelWebCrawlerTask with the provided parameters.
     *
     * @param url           the URL to crawl
     * @param deadline      the crawling deadline
     * @param maxDepth      the maximum depth for crawling
     * @param wordCounts    the concurrent map to store word counts
     * @param urlsVisited   the concurrent set to track visited URLs
     * @param parserFactory the factory for creating page parsers
     * @param clock         the clock used to track time
     * @param ignoredUrls   the list of ignored URL patterns
     */
    public ParallelWebCrawlerTask(String url, Instant deadline, int maxDepth, ConcurrentMap<String, Integer> wordCounts,
            ConcurrentSkipListSet<String> urlsVisited, PageParserFactory parserFactory,
            Clock clock, List<Pattern> ignoredUrls) {
        this.url = url;
        this.deadline = deadline;
        this.currentDepth = maxDepth;
        this.wordCounts = wordCounts;
        this.urlsVisited = urlsVisited;
        this.parserFactory = parserFactory;
        this.clock = clock;
        this.urlsIgnored = ignoredUrls;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Computes the crawling task for the given URL.
     *
     * @return {@code true} if the crawling task is successful, {@code false} otherwise.
     */
    @Override
    protected Boolean compute() {
        return Stream.of(url)
                // Filter out URLs if depth is 0 or deadline is passed
                .filter(u -> currentDepth != 0 && clock.instant().isBefore(deadline))
                // Filter out URLs that match any of the ignored patterns
                .filter(u -> urlsIgnored.stream().noneMatch(pattern -> pattern.matcher(u).matches()))
                // Filter out url if is disallowed for crawling
                .filter(u -> !isExcludedByRobotsTxt(u))
                // Add the URL to the visited set and filter out if it was already visited
                .filter(urlsVisited::add)
                // Parse the page and get the result
                .map(u -> parserFactory.get(u).parse())
                .peek(result -> {
                    // Update word counts
                    result.getWordCounts().forEach((key, value) ->
                            wordCounts.compute(key, (k, v) -> (v == null) ? value : value + v));
                    // Create subtasks for child links and invoke them
                    result.getLinks().stream()
                            .map(link -> new ParallelWebCrawlerTask(link, deadline, currentDepth - 1, wordCounts,
                                    urlsVisited,
                                    parserFactory, clock, urlsIgnored))
                            .map(ParallelWebCrawlerTask::fork)
                            .forEach(ForkJoinTask::invoke);
                })
                // Find any result (optional, for completeness)
                .findAny().isPresent();
    }

    // Auxiliary method that checks if a given URL is excluded by the robots.txt file.
    // Usage: test with sample_config_disallow_robots.json and verify it visits zero urls.
    private boolean isExcludedByRobotsTxt(String url) {
        try {
            URL robotsTxtUrl = new URL(url + "/robots.txt");
            InputStream inputStream = robotsTxtUrl.openStream();

            // Read the robots.txt file line by line using a BufferedReader
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                // Check whether the line starts with the "Disallow" word
                Predicate<String> isDisallowedLine = line -> line.startsWith("Disallow:");
                Predicate<String> isExcludedUrl = line -> {
                    // Extract the excluded path from the line
                    String excludedPath = line.substring("Disallow:".length()).trim();
                    // Check if the URL ends with the excluded path or contains it with a trailing slash
                    return url.endsWith(excludedPath) || url.contains(excludedPath + "/");
                };

                // Check if any line in robots.txt disallows the URL

                // Read lines from the robots.txt file
                return reader.lines()
                        // Trim leading and trailing whitespace from each line
                        .map(String::trim)
                        // Filter out lines that start with "Disallow:"
                        .filter(isDisallowedLine)
                        // Check if any line excludes the URL
                        .anyMatch(isExcludedUrl);

            }
        } catch (IOException e) {
            // Error occurred while accessing the robots.txt file,
            // consider the URL as not excluded
            return false;
        }
    }

}
