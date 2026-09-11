package dev.romildesai.modernization.modern;

import dev.romildesai.modernization.common.Money;
import dev.romildesai.modernization.legacy.LegacyBalanceCalculator;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Runnable walkthrough. No framework, no network, synthetic data only:
 *
 * <pre>{@code
 *   javac -d target/classes $(find src/main/java -name '*.java')
 *   java -cp target/classes dev.romildesai.modernization.modern.Demo
 * }</pre>
 */
public final class Demo {

    public static void main(String[] args) throws Exception {
        Instant now = Instant.parse("2026-01-15T10:00:00Z");
        List<LedgerEvent> events = List.of(
                new LedgerEvent.Deposit("ACCT-1001", Money.of("2500.00", "USD"), now),
                new LedgerEvent.Withdrawal("ACCT-1001", Money.of("300.00", "USD"), now.plusSeconds(60)),
                new LedgerEvent.Transfer("ACCT-1001", "ACCT-2002", Money.of("125.50", "USD"), now.plusSeconds(120)),
                new LedgerEvent.Reversal("ACCT-1001", "EVT-0003", Money.of("125.50", "USD"), now.plusSeconds(180)));

        System.out.println("=== 1. Sealed interface + pattern-matching switch ===");
        events.forEach(e -> System.out.println("  " + BalanceProjector.describe(e)));

        Money modern = BalanceProjector.project("USD", events);
        Money legacy = LegacyBalanceCalculator.project("USD", events);
        System.out.println("  modern projector : " + modern);
        System.out.println("  legacy projector : " + legacy);
        System.out.println("  parity           : " + modern.equals(legacy));

        System.out.println();
        System.out.println("=== 2. Virtual threads for I/O fan-out ===");
        List<String> accountIds = IntStream.rangeClosed(1, 500)
                .mapToObj(i -> String.format("ACCT-%04d", i))
                .toList();

        StatementFetcher fetcher = new StatementFetcher(
                id -> "statement:" + id, Duration.ofMillis(50));

        long start = System.nanoTime();
        List<String> statements = fetcher.fetchAll(accountIds);
        long millis = (System.nanoTime() - start) / 1_000_000;

        System.out.println("  fetched " + statements.size() + " statements in " + millis + " ms");
        System.out.println("  each call blocks 50 ms; sequentially this would be "
                + (accountIds.size() * 50) + " ms");
        System.out.println("  first result: " + statements.get(0));
    }
}
