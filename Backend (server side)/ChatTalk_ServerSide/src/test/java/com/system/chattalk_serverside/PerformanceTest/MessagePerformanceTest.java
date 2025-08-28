package com.system.chattalk_serverside.PerformanceTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple performance smoke test. Disabled by default.
 * Replace with a proper benchmark (JMH/Gatling) in a perf profile for realistic results.
 */
public class MessagePerformanceTest {

    @Test
    @Disabled("Performance smoke test - enable manually when needed")
    void parallel_counter_underLoad() throws InterruptedException {
        int threads = 16;
        int operations = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger counter = new AtomicInteger();

        Instant start = Instant.now();
        for (int i = 0; i < operations; i++) {
            pool.submit(counter::incrementAndGet);
        }
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        Instant end = Instant.now();

        Duration took = Duration.between(start, end);
        assert counter.get() == operations : "Not all operations completed";
        System.out.println("Perf smoke ran " + operations + " ops in " + took.toMillis() + " ms with " + threads + " threads");
    }
}


