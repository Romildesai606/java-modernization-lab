package dev.romildesai.modernization.legacy;

import dev.romildesai.modernization.common.Money;
import dev.romildesai.modernization.modern.LedgerEvent;

import java.util.List;

/**
 * The Java 8 shape of {@code BalanceProjector}, kept side by side so the diff is
 * reviewable. Two defects are structural rather than accidental:
 *
 * <ol>
 *   <li>the {@code instanceof} chain compiles happily when a new event type is
 *       added and silently falls through to the {@code else}; and</li>
 *   <li>every branch needs an explicit cast, which is where the copy-paste bugs
 *       land during a real migration.</li>
 * </ol>
 *
 * <p>This class is deliberately not deleted. On a modernization engagement the
 * legacy path stays in the tree behind a flag until the new path has been
 * verified against production traffic.</p>
 */
public final class LegacyBalanceCalculator {

    private LegacyBalanceCalculator() {
    }

    public static Money project(String currencyCode, List<LedgerEvent> events) {
        Money balance = Money.zero(currencyCode);
        for (LedgerEvent event : events) {
            if (event instanceof LedgerEvent.Deposit) {
                LedgerEvent.Deposit deposit = (LedgerEvent.Deposit) event;
                balance = balance.plus(deposit.amount());
            } else if (event instanceof LedgerEvent.Withdrawal) {
                LedgerEvent.Withdrawal withdrawal = (LedgerEvent.Withdrawal) event;
                balance = balance.minus(withdrawal.amount());
            } else if (event instanceof LedgerEvent.Transfer) {
                LedgerEvent.Transfer transfer = (LedgerEvent.Transfer) event;
                balance = balance.minus(transfer.amount());
            } else if (event instanceof LedgerEvent.Reversal) {
                LedgerEvent.Reversal reversal = (LedgerEvent.Reversal) event;
                balance = balance.plus(reversal.amount());
            }
            // No else branch. A new event type is silently ignored here.
        }
        return balance;
    }
}
