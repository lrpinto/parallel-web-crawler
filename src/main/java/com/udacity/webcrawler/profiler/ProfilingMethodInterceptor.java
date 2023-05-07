package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

    private final Clock clock;
    private final Object delegate;
    private final ZonedDateTime startTime;
    private final ProfilingState state;

    /**
     * Constructs a {@link ProfilingMethodInterceptor} with the provided dependencies.
     *
     * @param clock     the clock used for measuring time
     * @param delegate  the delegate object being profiled
     * @param state     the profiling state to record method durations
     * @param startTime the start time of the profiling
     */
    ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState state, ZonedDateTime startTime) {
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
        this.delegate = delegate;
        this.state = state;
        this.startTime = startTime;
    }

    /**
     * Intercepts the method invocation and performs profiling if the method is annotated with {@link Profiled}.
     * The method is invoked on the delegate object, and the duration of the invocation is recorded in the
     * {@link ProfilingState}.
     *
     * @param proxy  the proxy object
     * @param method the method being invoked
     * @param args   the arguments to the method
     * @return the result of the method invocation
     * @throws Throwable if an error occurs during the method invocation
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Determine whether the method is annotated with the profiled annotation
        boolean isProfiled = method.isAnnotationPresent(Profiled.class);
        // Invoke the method with profiling if profiled, otherwise invoke it without profiling
        return isProfiled ? profiledInvocation(method, args) : method.invoke(delegate, args);
    }

    // Auxiliary method that performs a profile invocation on the delegate object
    private Object profiledInvocation(Method method, Object[] args) throws Throwable {
        // Get the current instant as the start time for profiling
        Instant start = clock.instant();

        try {
            // Invoke the method on the delegate object, and return the result of the method invocation
            Object result = method.invoke(delegate, args);
            return result;
        } catch (InvocationTargetException ex) {
            // Throw the original exception that was thrown by the method
            throw ex.getTargetException();
        } finally {
            // Finally, record the duration, calling count, and thread ID
            recordInvocation(method, start);
        }
    }

    // Auxiliary method to record the duration, calling count, and thread ID of the method invocation on the delegate object
    private void recordInvocation(Method method, Instant start) {
        // Calculate the duration of the method invocation
        Duration duration = Duration.between(start, clock.instant());
        // Get the current thread ID
        long threadId = Thread.currentThread().getId();
        // Record the duration, calling count, and thread ID in the profiling state
        state.record(delegate.getClass(), method, duration, 1, threadId);
    }

}
