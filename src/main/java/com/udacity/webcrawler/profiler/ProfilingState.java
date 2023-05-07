package com.udacity.webcrawler.profiler;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Helper class that records method performance data from the method interceptor.
 */
final class ProfilingState {
    private final Map<String, AtomicLong> callCount = new ConcurrentHashMap<>();
    private final Map<String, Duration> data = new ConcurrentHashMap<>();
    private final Map<String, Long> threadIdMap = new ConcurrentHashMap<>();

    void record(Class<?> callingClass, Method method, Duration elapsed) {
        record(callingClass, method, elapsed, 1, Thread.currentThread().getId());
    }

    void record(Class<?> callingClass, Method method, Duration elapsed, long invocationCount, long threadId) {
        Objects.requireNonNull(callingClass);
        Objects.requireNonNull(method);
        Objects.requireNonNull(elapsed);
        if (elapsed.isNegative()) {
            throw new IllegalArgumentException("negative elapsed time");
        }
        String key = formatMethodCall(callingClass, method);
        data.compute(key, (k, v) -> (v == null) ? elapsed : v.plus(elapsed));
        callCount.computeIfAbsent(key, k -> new AtomicLong()).addAndGet(invocationCount);
        threadIdMap.put(key, threadId);
    }

    void write(Writer writer) throws IOException {
        List<String> entries = data.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String methodCall = entry.getKey();
                    Duration duration = entry.getValue();
                    long count = callCount.get(methodCall).get();
                    long threadId = threadIdMap.get(methodCall);
                    return methodCall + " took " + formatDuration(duration) +
                            " (called " + count + " times) [Thread ID: " + threadId + "]";
                })
                .collect(Collectors.toList());

        for (String entry : entries) {
            writer.write(entry + System.lineSeparator());
        }
    }

    private static String formatMethodCall(Class<?> callingClass, Method method) {
        return String.format("%s#%s", callingClass.getName(), method.getName());
    }

    private static String formatDuration(Duration duration) {
        return String.format(
                "%sm %ss %sms", duration.toMinutes(), duration.toSecondsPart(), duration.toMillisPart());
    }
}
