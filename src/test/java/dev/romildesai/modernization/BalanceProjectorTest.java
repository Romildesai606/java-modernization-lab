package dev.romildesai.modernization;

import dev.romildesai.modernization.common.Money;
import dev.romildesai.modernization.legacy.LegacyBalanceCalculator;
import dev.romildesai.modernization.modern.BalanceProjector;
import dev.romildesai.modernization.modern.LedgerEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceProjectorTest {

    private static final Instant T0 = Instant.parse("2026-01-15T10:00:00Z");

    private static List<LedgerEvent> sampleEvents() {
        return List.of(
                new LedgerEvent.Deposit("ACCT-1001", Money.of("2500.00", "USD"), T0),
                new LedgerEvent.Withdrawal("ACCT-1001", Money.of("300.00", "USD"), T0.plusSeconds(60)),
                new LedgerEvent.Transfer("ACCT-1001", "ACCT-2002", Money.of("125.50", "USD"), T0.plusSeconds(120)),
                new LedgerEvent.Reversal("ACCT-1001", "EVT-0003", Money.of("125.50", "USD"), T0.plusSeconds(180)));
    }

    @Test
    @DisplayName("modern projector folds every event type into the expected balance")
    void projectsExpectedBalance() {
        assertEquals(Money.of("2200.00", "USD"), BalanceProjector.project("USD", sampleEvents()));
    }

    @Test
    @DisplayName("modern and legacy implementations agree — the migration safety net")
    void legacyAndModernAgree() {
        assertEquals(
                LegacyBalanceCalculator.project("USD", sampleEvents()),
                BalanceProjector.project("USD", sampleEvents()));
    }

    @Test
    @DisplayName("empty event stream yields a zero balance, not null")
    void emptyStreamIsZero() {
        assertEquals(Money.zero("USD"), BalanceProjector.project("USD", List.of()));
    }

    @Test
    @DisplayName("guarded pattern flags a negative withdrawal instead of applying it silently")
    void guardedPatternDetectsNegativeWithdrawal() {
        LedgerEvent bad = new LedgerEvent.Withdrawal("ACCT-1001", Money.of("-10.00", "USD"), T0);
        assertTrue(BalanceProjector.describe(bad).startsWith("invalid withdrawal"));
    }

    @Test
    @DisplayName("mixing currencies is rejected at the value type, not discovered in reporting")
    void currencyMismatchIsRejected() {
        Money usd = Money.of("10.00", "USD");
        Money eur = Money.of("10.00", "EUR");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> usd.plus(eur));
        assertTrue(ex.getMessage().contains("Currency mismatch"));
    }

    @Test
    @DisplayName("money rounds half-even to the currency's minor units")
    void moneyRoundsHalfEven() {
        assertEquals("USD 2.22", Money.of("2.225", "USD").toString());
        assertEquals("USD 2.24", Money.of("2.235", "USD").toString());
    }
}
