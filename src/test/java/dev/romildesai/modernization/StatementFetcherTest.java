package dev.romildesai.modernization;

import dev.romildesai.modernization.modern.StatementFetcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementFetcherTest {

    @Test
    @DisplayName("500 blocking calls of 50 ms each complete far below the 25 s sequential cost")
    void virtualThreadsCollapseIoWallClock() throws Exception {
        List<String> ids = IntStream.rangeClosed(1, 500)
                .mapToObj(i -> String.format("ACCT-%04d", i))
                .toList();

        StatementFetcher fetcher = new StatementFetcher(id -> "statement:" + id, Duration.ofMillis(50));

        long start = System.nanoTime();
        List<String> statements = fetcher.fetchAll(ids);
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertEquals(500, statements.size());
        assertEquals("statement:ACCT-0001", statements.get(0));
        // Generous bound so the test is not flaky on a loaded CI runner.
        assertTrue(millis < 5_000, "expected well under the 25 s sequential cost, took " + millis + " ms");
    }
}
