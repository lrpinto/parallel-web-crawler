package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler} interface.
 * The {@code ProfilerImpl} provides methods for wrapping objects with profiling capabilities
 * and writing profiling data to a file or writer.
 */
final class ProfilerImpl implements Profiler {

    private final Clock clock;
    private final ZonedDateTime startTime;
    private final ProfilingState state = new ProfilingState();

    /**
     * Constructs a {@code ProfilerImpl} with the provided clock.
     *
     * @param clock the clock used for profiling.
     */
    @Inject
    ProfilerImpl(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
        this.startTime = ZonedDateTime.now(clock);
    }

    @Override
    public <T> T wrap(Class<T> klass, T delegate) {
        Objects.requireNonNull(klass, "Class must not be null");
        validateProfiledMethods(klass);

        Object proxy = Proxy.newProxyInstance(
                ProfilerImpl.class.getClassLoader(),
                new Class[]{klass},
                (proxyObj, method, args) ->
                        new ProfilingMethodInterceptor(clock, delegate, state, startTime)
                                .invoke(proxyObj, method, args)
        );

        return klass.cast(proxy);
    }

    /**
     * Validates if the given class has any methods annotated with {@link Profiled}.
     *
     * @param klass the class to check for profiled methods.
     * @throws IllegalArgumentException if the class does not have any profiled methods.
     */
    private void validateProfiledMethods(Class<?> klass) {
        boolean hasProfiledMethods = hasProfiledMethods(klass);
        if (!hasProfiledMethods) {
            throw new IllegalArgumentException(klass.getName() + " must have profiled methods.");
        }
    }

    /**
     * Checks if the given class has any methods annotated with {@link Profiled}.
     *
     * @param klass the class to check for profiled methods.
     * @return true if the class has profiled methods, false otherwise.
     */
    private boolean hasProfiledMethods(Class<?> klass) {
        return Arrays.stream(klass.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(Profiled.class));
    }

    @Override
    public void writeData(Path path) {
        Objects.requireNonNull(path, "Path must not be null");

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, CREATE, APPEND)) {
            writeData(writer);
            writer.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void writeData(Writer writer) throws IOException {
        writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
        writer.write(System.lineSeparator());
        state.write(writer);
        writer.write(System.lineSeparator());
    }
}
