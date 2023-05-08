package com.udacity.webcrawler.profiler;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Helper class that records method performance data from the method interceptor.
 */
public final class ProfilingState {
    private final Map<String, Map<Long, AtomicLong>> callCountsByThread = new ConcurrentHashMap<>();
    private final Map<String, Duration> totalDurations = new ConcurrentHashMap<>();

    /**
     * Records the method performance data.
     *
     * @param callingClass The class that called the method.
     * @param method       The method being called.
     * @param elapsed      The duration of the method call.
     * @param threadId     The ID of the thread executing the method call.
     * @throws NullPointerException     if any of the arguments is null.
     * @throws IllegalArgumentException if the elapsed time is negative.
     */
    public void record(Class<?> callingClass, Method method, Duration elapsed, long threadId) {
        Objects.requireNonNull(callingClass);
        Objects.requireNonNull(method);
        Objects.requireNonNull(elapsed);

        if (elapsed.isNegative()) {
            throw new IllegalArgumentException("negative elapsed time");
        }
        String key = formatMethodCall(callingClass, method);

        callCountsByThread
                .computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(threadId, t -> new AtomicLong())
                .addAndGet(1);

        totalDurations.compute(key, (k, v) -> (v == null) ? elapsed : v.plus(elapsed));
    }

    /**
     * Formats the method call as a string in the format "ClassName#methodName".
     *
     * @param callingClass The class that called the method.
     * @param method       The method being called.
     * @return The formatted method call string.
     */
    public static String formatMethodCall(Class<?> callingClass, Method method) {
        return String.format("%s#%s", callingClass.getName(), method.getName());
    }

    /**
     * Writes the recorded method performance data to a writer.
     *
     * @param writer The writer to write the data to.
     * @throws IOException If an I/O error occurs while writing.
     */
    public void write(Writer writer) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        callCountsByThread.forEach((methodCall, threadCounts) -> {
            // Calculate total invocations for this method across all threads
            long totalInvocations = threadCounts.values().stream()
                    .mapToLong(AtomicLong::get)
                    .sum();

            // Calculate total duration for this method across all threads
            Duration totalDuration = totalDurations.get(methodCall);

            // Append method information
            stringBuilder.append(methodCall)
                    .append(" took ")
                    .append(formatDuration(totalDuration))
                    .append(" (called ")
                    .append(totalInvocations)
                    .append(" times)")
                    .append(System.lineSeparator());

            // Append invocations and average duration per thread
            threadCounts.forEach((threadId, invocations) -> {
                // Calculate average duration per thread
                Duration threadTotalDuration = totalDurations.get(methodCall);
                Duration threadAverageDuration = threadTotalDuration.dividedBy(invocations.get());

                stringBuilder.append("[Thread ID: ")
                        .append(threadId)
                        .append(" (called ")
                        .append(invocations.get())
                        .append(" times)] - Average duration: ")
                        .append(formatDuration(threadAverageDuration))
                        .append(System.lineSeparator());
            });

            stringBuilder.append(System.lineSeparator());
        });

        writer.write(stringBuilder.toString());
    }

    /**
     * Formats the duration as a string in the format "m minutes s seconds ms milliseconds".
     *
     * @param duration The duration to format.
     * @return The formatted duration string.
     */
    public static String formatDuration(Duration duration) {
        return String.format(
                "%sm %ss %sms", duration.toMinutes(), duration.toSecondsPart(), duration.toMillisPart());
    }
}
