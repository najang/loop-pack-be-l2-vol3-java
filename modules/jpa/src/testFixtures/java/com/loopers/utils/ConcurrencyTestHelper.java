package com.loopers.utils;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public final class ConcurrencyTestHelper {

    public record ConcurrencyResult(long successCount, long failureCount) {
        public long total() {
            return successCount + failureCount;
        }
    }

    public static ConcurrencyResult run(int threadCount, Callable<Object> task) throws Exception {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Future<Boolean>> futures = IntStream.range(0, threadCount)
            .mapToObj(i -> executor.submit(toCallable(ready, start, task)))
            .toList();

        ready.await();
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        long success = countSuccesses(futures);
        return new ConcurrencyResult(success, threadCount - success);
    }

    public static ConcurrencyResult run(List<Callable<Object>> tasks) throws Exception {
        int threadCount = tasks.size();
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Future<Boolean>> futures = tasks.stream()
            .map(task -> executor.submit(toCallable(ready, start, task)))
            .toList();

        ready.await();
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        long success = countSuccesses(futures);
        return new ConcurrencyResult(success, threadCount - success);
    }

    private static Callable<Boolean> toCallable(CountDownLatch ready, CountDownLatch start, Callable<Object> task) {
        return () -> {
            ready.countDown();
            start.await();
            try {
                task.call();
                return true;
            } catch (Exception e) {
                return false;
            }
        };
    }

    private static long countSuccesses(List<Future<Boolean>> futures) {
        return futures.stream()
            .mapToLong(f -> {
                try {
                    return f.get() ? 1L : 0L;
                } catch (Exception e) {
                    return 0L;
                }
            })
            .sum();
    }
}
