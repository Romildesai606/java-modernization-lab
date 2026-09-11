package dev.romildesai.modernization.modern;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Fan-out over blocking downstream calls.
 *
 * <p>The Java 8 version of this sat on a fixed thread pool sized by trial and error:
 * too small and the batch ran long, too large and the box ran out of memory holding
 * platform threads that were only ever parked on I/O. Virtual threads
 * ({@code Executors.newVirtualThreadPerTaskExecutor()}, JEP 444, GA in Java 21)
 * remove the sizing decision for I/O-bound fan-out — one thread per task, cheap
 * enough that the pool size stops being a tuning parameter.</p>
 *
 * <p>The important caveat, and the one that actually bites during a migration:
 * this only helps for <em>blocking I/O</em>. CPU-bound work still needs a bounded
 * pool, and a {@code synchronized} block around a blocking call pins the carrier
 * thread and undoes the benefit — prefer {@link java.util.concurrent.locks.ReentrantLock}.</p>
 */
public final class StatementFetcher {

    private final Function<String, String> downstream;
    private final Duration simulatedLatency;

    public StatementFetcher(Function<String, String> downstream, Duration simulatedLatency) {
        this.downstream = downstream;
        this.simulatedLatency = simulatedLatency;
    }

    public List<String> fetchAll(List<String> accountIds) throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<String>> tasks = new ArrayList<>(accountIds.size());
            for (String accountId : accountIds) {
                tasks.add(() -> {
                    Thread.sleep(simulatedLatency);
                    return downstream.apply(accountId);
                });
            }
            List<Future<String>> futures = executor.invokeAll(tasks);
            List<String> results = new ArrayList<>(futures.size());
            for (Future<String> future : futures) {
                results.add(future.get());
            }
            return results;
        }
    }
}
